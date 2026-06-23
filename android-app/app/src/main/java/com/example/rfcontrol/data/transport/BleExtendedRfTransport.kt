package com.example.rfcontrol.data.transport

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.example.rfcontrol.data.protocol.DeviceStatus
import com.example.rfcontrol.data.protocol.RfDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Placeholder for the production BLE 5 Extended Advertising transport.
 *
 * The first implementation round intentionally uses MockRfTransport. When hardware
 * integration starts, this class should wrap BluetoothLeAdvertiser.startAdvertisingSet
 * and BluetoothLeScanner, then fail fast when the phone does not support extended
 * advertising.
 */
class BleExtendedRfTransport(context: Context) : RfTransport {
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter

    val isExtendedAdvertisingSupported: Boolean
        get() = adapter?.isLeExtendedAdvertisingSupported == true

    override val scannedDevices: Flow<List<RfDevice>> = emptyFlow()
    override val deviceStatuses: Flow<DeviceStatus> = emptyFlow()

    override suspend fun startAdvertising(packetProvider: () -> ByteArray) {
        check(isExtendedAdvertisingSupported) {
            "当前手机不支持 BLE Extended Advertising，无法完整发送 42 字节 RF 控制包。"
        }
        TODO("Wire BluetoothLeAdvertiser.startAdvertisingSet in the hardware integration phase.")
    }

    override suspend fun stopAdvertising() = Unit
    override suspend fun startScanning() = Unit
    override suspend fun stopScanning() = Unit
}
