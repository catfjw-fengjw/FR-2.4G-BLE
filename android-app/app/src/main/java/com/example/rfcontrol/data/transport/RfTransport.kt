package com.example.rfcontrol.data.transport

import com.example.rfcontrol.data.protocol.DeviceStatus
import com.example.rfcontrol.data.protocol.RfDevice
import kotlinx.coroutines.flow.Flow

interface RfTransport {
    val scannedDevices: Flow<List<RfDevice>>
    val deviceStatuses: Flow<DeviceStatus>
    val transportEvents: Flow<TransportEvent>
    suspend fun startAdvertising(packetProvider: () -> ByteArray)
    suspend fun stopAdvertising()
    suspend fun startScanning(expectedDeviceIdProvider: () -> String)
    suspend fun stopScanning()
}

data class TransportEvent(
    val type: TransportEventType,
    val message: String
)

enum class TransportEventType {
    Info,
    Ok,
    Warn,
    Error,
    Tx,
    Rx
}
