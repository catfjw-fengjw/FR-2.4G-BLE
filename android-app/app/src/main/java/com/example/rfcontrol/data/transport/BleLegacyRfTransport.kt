package com.example.rfcontrol.data.transport

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.example.rfcontrol.data.protocol.DeviceStatus
import com.example.rfcontrol.data.protocol.RfDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Placeholder for real BLE legacy advertising.
 *
 * Protocol clarification:
 * - The full RF/BLE over-air packet is 42 bytes.
 * - Android app code should not try to submit all 42 bytes as advertising data.
 * - The BLE controller owns the 2-byte PDU header, 6-byte advertiser address,
 *   and 3-byte link-layer CRC.
 * - The app-facing payload is Byte9~Byte39, a 31-byte legacy AdvData section.
 *
 * Important Android API note:
 * Android's public BluetoothLeAdvertiser API builds advertising data from AD
 * structures; it does not expose a raw "set exactly these 31 AdvData bytes" API.
 * Hardware integration must confirm whether the receiver accepts the Android
 * generated AD structures or whether a vendor/RF module path is required for
 * byte-exact AdvData.
 */
class BleLegacyRfTransport(context: Context) : RfTransport {
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter

    val isLegacyAdvertisingSupported: Boolean
        get() = adapter?.bluetoothLeAdvertiser != null

    override val scannedDevices: Flow<List<RfDevice>> = emptyFlow()
    override val deviceStatuses: Flow<DeviceStatus> = emptyFlow()

    override suspend fun startAdvertising(packetProvider: () -> ByteArray) {
        val overAirPacket = packetProvider()
        val advData = overAirPacket.sliceArray(8..38)
        check(advData.size == 31) { "Legacy AdvData must be 31 bytes." }
        check(isLegacyAdvertisingSupported) { "当前手机不支持 BLE 广播发送。" }
        TODO("Wire BluetoothLeAdvertiser after confirming exact AD-structure mapping with receiver firmware.")
    }

    override suspend fun stopAdvertising() = Unit
    override suspend fun startScanning() = Unit
    override suspend fun stopScanning() = Unit
}
