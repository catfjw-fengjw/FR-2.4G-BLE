package com.example.rfcontrol.data.transport

import com.example.rfcontrol.data.protocol.DeviceStatus
import com.example.rfcontrol.data.protocol.RfDevice
import kotlinx.coroutines.flow.Flow

interface RfTransport {
    val scannedDevices: Flow<List<RfDevice>>
    val deviceStatuses: Flow<DeviceStatus>
    suspend fun startAdvertising(packetProvider: () -> ByteArray)
    suspend fun stopAdvertising()
    suspend fun startScanning()
    suspend fun stopScanning()
}
