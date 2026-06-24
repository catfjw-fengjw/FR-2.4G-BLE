package com.example.rfcontrol.data.transport

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.rfcontrol.data.protocol.DeviceStatus
import com.example.rfcontrol.data.protocol.RfDevice
import com.example.rfcontrol.data.protocol.RfPacketBuilder
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Real BLE advertising debug transport.
 *
 * Android's public BluetoothLeAdvertiser does not provide a raw AdvData API.
 * The protocol's Byte9~Byte39 31-byte AdvData cannot be injected byte-for-byte.
 * For phone-side debugging this class broadcasts a compact manufacturer data
 * carrier that contains the core protocol fields:
 *
 * RF 01 + Byte14~Byte29
 *
 * This is small enough for legacy advertising and can be inspected by a BLE
 * scanner while the firmware/receiver mapping is confirmed.
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

    private var advertiseCallback: AdvertiseCallback? = null

    val isBleSupported: Boolean
        get() = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    val isBluetoothEnabled: Boolean
        get() = adapter?.isEnabled == true

    val isLegacyAdvertisingSupported: Boolean
        get() = advertiser != null

    fun capabilitySummary(): String {
        return "BLE=${yesNo(isBleSupported)} 蓝牙=${yesNo(isBluetoothEnabled)} 广播=${yesNo(isLegacyAdvertisingSupported)}"
    }

    @SuppressLint("MissingPermission")
    override suspend fun startAdvertising(packetProvider: () -> ByteArray) {
        val activeAdvertiser = advertiser ?: error("当前手机不支持 BLE 广播发送或蓝牙未开启。")
        if (!isBleSupported) error("当前手机未声明 BLE 硬件能力。")
        if (!isBluetoothEnabled) error("蓝牙未开启，请先打开蓝牙。")
        if (!hasAdvertisePermission()) error("缺少蓝牙广播权限，请允许附近设备/蓝牙权限。")

        stopAdvertising()

        val overAirPacket = packetProvider()
        check(overAirPacket.size == RfPacketBuilder.OverAirPacketSize) {
            "空中包预览必须是 ${RfPacketBuilder.OverAirPacketSize} 字节。"
        }

        val legacyAdvData = overAirPacket.sliceArray(8..38)
        check(legacyAdvData.size == RfPacketBuilder.LegacyAdvDataSize) {
            "Legacy AdvData 必须是 ${RfPacketBuilder.LegacyAdvDataSize} 字节。"
        }

        val debugPayload = buildDebugManufacturerPayload(overAirPacket)
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(0)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addManufacturerData(DebugManufacturerId, debugPayload)
            .build()

        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                _transportEvents.tryEmit(
                    TransportEvent(
                        TransportEventType.Ok,
                        "真机 BLE 调试广播已启动，Manufacturer ID=${idHex()}，Payload=${hex(debugPayload)}。"
                    )
                )
                _transportEvents.tryEmit(
                    TransportEvent(
                        TransportEventType.Warn,
                        "Android 不能发送原始 31 字节 AdvData；当前广播承载 RF 01 + Byte14~Byte29，用于抓包调试。"
                    )
                )
            }

            override fun onStartFailure(errorCode: Int) {
                _transportEvents.tryEmit(
                    TransportEvent(
                        TransportEventType.Error,
                        "真机 BLE 广播启动失败：${advertiseErrorText(errorCode)}。"
                    )
                )
            }
        }

        advertiseCallback = callback
        activeAdvertiser.startAdvertising(settings, data, callback)
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopAdvertising() {
        val callback = advertiseCallback ?: return
        if (hasAdvertisePermission()) {
            advertiser?.stopAdvertising(callback)
        }
        advertiseCallback = null
        _transportEvents.tryEmit(TransportEvent(TransportEventType.Info, "真机 BLE 调试广播已停止。"))
    }

    override suspend fun startScanning() = Unit
    override suspend fun stopScanning() = Unit

    private fun hasAdvertisePermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_ADVERTISE) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun buildDebugManufacturerPayload(overAirPacket: ByteArray): ByteArray {
        val coreProtocol = overAirPacket.sliceArray(13..28)
        return byteArrayOf('R'.code.toByte(), 'F'.code.toByte(), 0x01) + coreProtocol
    }

    private fun advertiseErrorText(errorCode: Int): String {
        return when (errorCode) {
            AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "已经在广播"
            AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "广播数据过大"
            AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "手机不支持该广播功能"
            AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "蓝牙栈内部错误"
            AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "广播实例过多"
            else -> "未知错误码 $errorCode"
        }
    }

    private fun yesNo(value: Boolean): String = if (value) "OK" else "NO"

    private fun idHex(): String = "0x%04X".format(Locale.US, DebugManufacturerId)

    private fun hex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(Locale.US, it.toInt() and 0xFF) }
    }

    private companion object {
        const val DebugManufacturerId = 0xFFFF
    }
}
