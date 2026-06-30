package com.example.rfcontrol.data.transport

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.rfcontrol.data.protocol.DeviceStatus
import com.example.rfcontrol.data.protocol.RfDevice
import com.example.rfcontrol.data.protocol.RfPacketBuilder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Real BLE advertising debug transport.
 *
 * Android can request a legacy, non-connectable, non-scannable advertising set
 * so the link-layer PDU type targets ADV_NONCONN_IND. The public API still
 * cannot inject arbitrary raw AdvData bytes. This transport therefore sets the
 * Bluetooth local name to the protocol name field and asks Android to emit the
 * V1.6 Complete Local Name and Manufacturer Specific Data structures.
 */
class BleDebugRfTransport(context: Context) : RfTransport {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val advertiser
        get() = adapter?.bluetoothLeAdvertiser

    private val _transportEvents = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 32)
    override val transportEvents: Flow<TransportEvent> = _transportEvents
    override val scannedDevices: Flow<List<RfDevice>> = emptyFlow()
    override val deviceStatuses: Flow<DeviceStatus> = emptyFlow()

    private var advertisingSetCallback: AdvertisingSetCallback? = null
    private var originalBluetoothName: String? = null

    val isBleSupported: Boolean
        get() = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    val isBluetoothEnabled: Boolean
        get() = adapter?.isEnabled == true

    val isLegacyAdvertisingSupported: Boolean
        get() = advertiser != null

    fun capabilitySummary(): String {
        return "BLE=${yesNo(isBleSupported)} 蓝牙=${yesNo(isBluetoothEnabled)} ADV_SET=${yesNo(isLegacyAdvertisingSupported)}"
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

    override suspend fun startScanning() = Unit
    override suspend fun stopScanning() = Unit

    private fun hasAdvertisePermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_ADVERTISE) ==
            PackageManager.PERMISSION_GRANTED
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
    }
}
