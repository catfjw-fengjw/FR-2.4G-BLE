package com.example.rfcontrol.data.transport

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.rfcontrol.data.protocol.DeviceStatus
import com.example.rfcontrol.data.protocol.RfDevice
import com.example.rfcontrol.data.protocol.RfPacketBuilder
import com.example.rfcontrol.data.protocol.RfPacketParser
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Real BLE advertising/debug transport.
 *
 * Advertising uses the Android public BLE API to emit the V1.7 Local Name +
 * Manufacturer Data structures. Scanning reconstructs a protocol preview packet
 * from ScanRecord fields because Android does not expose the raw 42-byte
 * link-layer packet to app code.
 */
class BleDebugRfTransport(context: Context) : RfTransport {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val advertiser
        get() = adapter?.bluetoothLeAdvertiser
    private val scanner
        get() = adapter?.bluetoothLeScanner

    private val _transportEvents = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 32)
    override val transportEvents: Flow<TransportEvent> = _transportEvents
    override val scannedDevices: Flow<List<RfDevice>> = emptyFlow()
    private val _deviceStatuses = MutableSharedFlow<DeviceStatus>(extraBufferCapacity = 16)
    override val deviceStatuses: Flow<DeviceStatus> = _deviceStatuses

    private var advertisingSetCallback: AdvertisingSetCallback? = null
    private var scanCallback: ScanCallback? = null
    private var originalBluetoothName: String? = null

    val isBleSupported: Boolean
        get() = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    val isBluetoothEnabled: Boolean
        get() = adapter?.isEnabled == true

    val isLegacyAdvertisingSupported: Boolean
        get() = advertiser != null

    val isScanSupported: Boolean
        get() = scanner != null

    fun capabilitySummary(): String {
        return "BLE=${yesNo(isBleSupported)} 蓝牙=${yesNo(isBluetoothEnabled)} ADV=${yesNo(isLegacyAdvertisingSupported)} SCAN=${yesNo(isScanSupported)}"
    }

    @SuppressLint("MissingPermission")
    override suspend fun startAdvertising(packetProvider: () -> ByteArray) {
        val activeAdvertiser = advertiser ?: error("当前手机不支持 BLE 广播发送，或蓝牙未开启。")
        if (!isBleSupported) error("当前手机未声明 BLE 硬件能力。")
        if (!isBluetoothEnabled) error("蓝牙未开启，请先打开蓝牙。")
        if (!hasAdvertisePermission()) error("缺少蓝牙广播权限，请允许附近设备/蓝牙权限。")

        stopAdvertising()

        val overAirPacket = packetProvider()
        check(overAirPacket.size == RfPacketBuilder.OverAirPacketSize) {
            "空中包预览必须是 ${RfPacketBuilder.OverAirPacketSize} 字节。"
        }

        val protocolAdvData = buildProtocolAdvData(overAirPacket)
        val localName = buildProtocolLocalName(protocolAdvData)
        updateBluetoothName(localName)

        val parametersBuilder = AdvertisingSetParameters.Builder()
            .setLegacyMode(true)
            .setConnectable(false)
            .setScannable(false)
            .setInterval(AdvertisingSetParameters.INTERVAL_LOW)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
        val discoverableSupported = trySetDiscoverable(parametersBuilder)
        val parameters = parametersBuilder.build()
        val carrierData = buildLocalNameAdvertiseData(protocolAdvData)

        val callback = object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
                if (status != ADVERTISE_SUCCESS) {
                    _transportEvents.tryEmit(
                        TransportEvent(
                            TransportEventType.Error,
                            "真机 ADV_NONCONN_IND 广播启动失败：${advertiseErrorText(status)}。"
                        )
                    )
                    return
                }
                _transportEvents.tryEmit(
                    TransportEvent(
                        TransportEventType.Tx,
                        "TYPE=ADV_NONCONN_IND Flags=${flagsText(discoverableSupported)} Name=$localName NameAd=${hex(buildNameAdStructure(protocolAdvData))} ManufacturerAd=${hex(buildManufacturerAdStructure(protocolAdvData))} AdvData=${hex(protocolAdvData)}"
                    )
                )
            }

            override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet?, status: Int) {
                if (status != ADVERTISE_SUCCESS) {
                    _transportEvents.tryEmit(
                        TransportEvent(
                            TransportEventType.Error,
                            "广播数据设置失败：${advertiseErrorText(status)}。"
                        )
                    )
                }
            }

            override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
                _transportEvents.tryEmit(TransportEvent(TransportEventType.Info, ""))
            }
        }

        advertisingSetCallback = callback
        activeAdvertiser.startAdvertisingSet(parameters, carrierData, null, null, null, callback)
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopAdvertising() {
        val callback = advertisingSetCallback ?: return
        if (hasAdvertisePermission()) {
            advertiser?.stopAdvertisingSet(callback)
        }
        advertisingSetCallback = null
        restoreBluetoothName()
        _transportEvents.tryEmit(TransportEvent(TransportEventType.Info, ""))
    }

    @SuppressLint("MissingPermission")
    override suspend fun startScanning(expectedDeviceIdProvider: () -> String) {
        val activeScanner = scanner ?: error("当前手机不支持 BLE 扫描，或蓝牙未开启。")
        if (!isBleSupported) error("当前手机未声明 BLE 硬件能力。")
        if (!isBluetoothEnabled) error("蓝牙未开启，请先打开蓝牙。")
        if (!hasScanPermission()) error("缺少蓝牙扫描权限，请允许附近设备/蓝牙权限。")

        stopScanning()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                emitDeviceStatusIfMatched(result, expectedDeviceIdProvider)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { emitDeviceStatusIfMatched(it, expectedDeviceIdProvider) }
            }

            override fun onScanFailed(errorCode: Int) {
                _transportEvents.tryEmit(
                    TransportEvent(TransportEventType.Error, "BLE 扫描启动失败：${scanErrorText(errorCode)}。")
                )
            }
        }

        scanCallback = callback
        activeScanner.startScan(null, buildScanSettings(), callback)
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopScanning() {
        val callback = scanCallback ?: return
        if (hasScanPermission()) {
            scanner?.stopScan(callback)
        }
        scanCallback = null
    }

    @SuppressLint("MissingPermission")
    private fun emitDeviceStatusIfMatched(result: ScanResult, expectedDeviceIdProvider: () -> String) {
        val record = result.scanRecord ?: return
        val manufacturerEntry = extractManufacturerPayload(record)
        val localName = record.deviceName
        Log.d(
            Tag,
            "scan mac=${result.device.address} rssi=${result.rssi} name=${localName ?: "<null>"} mfgCompany=${manufacturerEntry?.first ?: "none"} mfgBytes=${manufacturerEntry?.second?.size ?: 0}"
        )
        if (localName == null) return
        if (!localName.startsWith("LY") || localName.length != 8) return

        val expectedDeviceId = expectedDeviceIdProvider().uppercase(Locale.US)
        if (localName.substring(2) != expectedDeviceId) {
            Log.d(Tag, "scan ignored because id mismatch name=$localName expected=LY$expectedDeviceId")
            return
        }

        val (companyId, payload) = manufacturerEntry ?: run {
            Log.d(Tag, "scan ignored because manufacturer payload missing for name=$localName")
            return
        }
        val packet = buildDeviceStatusPreviewPacket(
            localName = localName,
            companyId = companyId,
            manufacturerPayload = payload,
            advertiserMac = result.device.address ?: RfPacketBuilder.DefaultSenderMac
        )

        val status = RfPacketParser.parseDeviceStatus(packet, result.rssi, expectedDeviceId = expectedDeviceId) ?: run {
            Log.d(Tag, "scan ignored because parser rejected packet=${hex(packet)}")
            return
        }
        Log.d(Tag, "scan accepted status battery=${status.battery} mac=${status.mac} packet=${status.packetHex}")
        _deviceStatuses.tryEmit(status)
    }

    private fun hasAdvertisePermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_ADVERTISE) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun advertiseErrorText(errorCode: Int): String {
        return when (errorCode) {
            AdvertisingSetCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "已经在广播"
            AdvertisingSetCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "广播数据过大"
            AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "手机不支持该广播功能"
            AdvertisingSetCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "蓝牙栈内部错误"
            AdvertisingSetCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "广播实例过多"
            else -> "未知错误码 $errorCode"
        }
    }

    private fun scanErrorText(errorCode: Int): String {
        return when (errorCode) {
            ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "扫描已经启动"
            ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "扫描注册失败"
            ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "手机不支持 BLE 扫描"
            ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "蓝牙扫描内部错误"
            else -> "未知错误码 $errorCode"
        }
    }

    private fun yesNo(value: Boolean): String = if (value) "OK" else "NO"

    private fun hex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(Locale.US, it.toInt() and 0xFF) }
    }

    private fun flagsText(discoverableSupported: Boolean): String {
        return if (discoverableSupported) "02 01 06" else "UNSUPPORTED_ON_ANDROID_${Build.VERSION.SDK_INT}"
    }

    @SuppressLint("MissingPermission")
    private fun updateBluetoothName(localName: String) {
        val activeAdapter = adapter ?: return
        if (originalBluetoothName == null) {
            originalBluetoothName = activeAdapter.name
        }
        if (activeAdapter.name != localName) {
            val updated = activeAdapter.setName(localName)
            check(updated) { "无法设置蓝牙名称为协议设备名：$localName" }
        }
    }

    @SuppressLint("MissingPermission")
    private fun restoreBluetoothName() {
        val name = originalBluetoothName ?: return
        adapter?.setName(name)
        originalBluetoothName = null
    }

    internal companion object {
        private const val Tag = "RfBleDebug"

        fun buildProtocolAdvData(overAirPacket: ByteArray): ByteArray {
            check(overAirPacket.size == RfPacketBuilder.OverAirPacketSize) {
                "空中包预览必须是 ${RfPacketBuilder.OverAirPacketSize} 字节。"
            }
            return overAirPacket.sliceArray(8..38).also {
                check(it.size == RfPacketBuilder.LegacyAdvDataSize) {
                    "Legacy AdvData 必须是 ${RfPacketBuilder.LegacyAdvDataSize} 字节。"
                }
            }
        }

        fun buildProtocolLocalName(protocolAdvData: ByteArray): String {
            check(protocolAdvData.size == RfPacketBuilder.LegacyAdvDataSize) {
                "协议 AdvData 必须是 ${RfPacketBuilder.LegacyAdvDataSize} 字节。"
            }
            check(protocolAdvData[0].toInt() == 0x09 && protocolAdvData[1].toInt() == 0x09) {
                "协议 AdvData 必须包含 09 09 名称字段。"
            }
            return String(protocolAdvData.sliceArray(2..9), StandardCharsets.US_ASCII)
        }

        fun buildLocalNameAdvertiseData(protocolAdvData: ByteArray): AdvertiseData {
            buildProtocolLocalName(protocolAdvData)
            return AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addManufacturerData(buildManufacturerCompanyId(protocolAdvData), buildManufacturerPayload(protocolAdvData))
                .build()
        }

        fun buildNameAdStructure(protocolAdvData: ByteArray): ByteArray {
            check(protocolAdvData.size == RfPacketBuilder.LegacyAdvDataSize) {
                "协议 AdvData 必须是 ${RfPacketBuilder.LegacyAdvDataSize} 字节。"
            }
            return protocolAdvData.sliceArray(0..9)
        }

        fun buildManufacturerAdStructure(protocolAdvData: ByteArray): ByteArray {
            check(protocolAdvData.size == RfPacketBuilder.LegacyAdvDataSize) {
                "协议 AdvData 必须是 ${RfPacketBuilder.LegacyAdvDataSize} 字节。"
            }
            return protocolAdvData.sliceArray(10..30)
        }

        fun buildManufacturerCompanyId(protocolAdvData: ByteArray): Int {
            val manufacturerAd = buildManufacturerAdStructure(protocolAdvData)
            check(manufacturerAd[0].toInt() == 0x14 && (manufacturerAd[1].toInt() and 0xFF) == 0xFF) {
                "协议 AdvData 必须包含 14 FF 厂商字段。"
            }
            return (manufacturerAd[2].toInt() and 0xFF) or ((manufacturerAd[3].toInt() and 0xFF) shl 8)
        }

        fun buildManufacturerPayload(protocolAdvData: ByteArray): ByteArray {
            val manufacturerAd = buildManufacturerAdStructure(protocolAdvData)
            check(manufacturerAd[0].toInt() == 0x14 && (manufacturerAd[1].toInt() and 0xFF) == 0xFF) {
                "协议 AdvData 必须包含 14 FF 厂商字段。"
            }
            return manufacturerAd.sliceArray(4..20)
        }

        fun buildDeviceStatusPreviewPacket(
            localName: String,
            companyId: Int,
            manufacturerPayload: ByteArray,
            advertiserMac: String
        ): ByteArray {
            require(localName.length == 8) { "设备名称必须是 8 字节，例如 LY111111。" }
            require(localName.startsWith("LY")) { "设备状态名称必须使用 LY 前缀。" }
            require(manufacturerPayload.size == 17) { "Manufacturer payload 必须是 17 字节。" }

            val packet = ByteArray(RfPacketBuilder.OverAirPacketSize)
            packet[0] = 0x42
            packet[1] = 0x25

            advertiserMac.split(":").map { it.toInt(16) }.forEachIndexed { index, value ->
                packet[2 + index] = value.toByte()
            }

            packet[8] = 0x09
            packet[9] = 0x09
            localName.toByteArray(StandardCharsets.US_ASCII).forEachIndexed { index, value ->
                packet[10 + index] = value
            }

            packet[18] = 0x14
            packet[19] = 0xFF.toByte()
            packet[20] = (companyId and 0xFF).toByte()
            packet[21] = ((companyId shr 8) and 0xFF).toByte()
            manufacturerPayload.forEachIndexed { index, value ->
                packet[22 + index] = value
            }

            packet[39] = 0x55
            packet[40] = 0x55
            packet[41] = 0x55
            return packet
        }

        fun trySetDiscoverable(builder: AdvertisingSetParameters.Builder): Boolean {
            return try {
                val method = builder.javaClass.getMethod("setDiscoverable", Boolean::class.javaPrimitiveType)
                method.invoke(builder, true)
                true
            } catch (_: ReflectiveOperationException) {
                false
            } catch (_: RuntimeException) {
                false
            }
        }

        private fun extractManufacturerPayload(record: ScanRecord): Pair<Int, ByteArray>? {
            val data = record.manufacturerSpecificData ?: return null
            if (data.size() == 0) return null
            val companyId = data.keyAt(0)
            val payload = data.valueAt(0) ?: return null
            if (payload.size != 17) return null
            return companyId to payload
        }

        private fun buildScanSettings(): ScanSettings {
            return ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
        }
    }
}
