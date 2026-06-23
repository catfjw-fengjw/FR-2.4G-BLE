package com.example.rfcontrol.data.transport

import com.example.rfcontrol.data.protocol.DeviceStatus
import com.example.rfcontrol.data.protocol.RfDevice
import com.example.rfcontrol.data.protocol.RfPacketBuilder
import com.example.rfcontrol.data.protocol.RfPacketParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MockRfTransport(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : RfTransport {
    private val devices = listOf(
        RfDevice("LX_DX001", "01:01:01:00:00:01", -46, 86, "刚刚"),
        RfDevice("LX_DX014", "01:01:02:00:00:0E", -63, 72, "4 秒前"),
        RfDevice("LX_DX108", "01:03:08:00:00:6C", -71, 48, "12 秒前")
    )

    private val _scannedDevices = MutableStateFlow(devices)
    override val scannedDevices: Flow<List<RfDevice>> = _scannedDevices.asStateFlow()

    private val _deviceStatuses = MutableSharedFlow<DeviceStatus>(extraBufferCapacity = 8)
    override val deviceStatuses: Flow<DeviceStatus> = _deviceStatuses.asSharedFlow()

    private var advertisingJob: Job? = null
    private var scanningJob: Job? = null
    private var battery = devices.first().battery
    private var rssi = devices.first().rssi

    override suspend fun startAdvertising(packetProvider: () -> ByteArray) {
        advertisingJob?.cancel()
        advertisingJob = scope.launch {
            while (isActive) {
                packetProvider()
                delay(10)
            }
        }
    }

    override suspend fun stopAdvertising() {
        advertisingJob?.cancel()
        advertisingJob = null
    }

    override suspend fun startScanning() {
        scanningJob?.cancel()
        scanningJob = scope.launch {
            while (isActive) {
                delay(5_000)
                battery = (battery + listOf(-1, 1).random()).coerceIn(30, 100)
                rssi = (rssi + (-3..3).random()).coerceIn(-82, -35)
                val packet = RfPacketBuilder.buildDevicePacket(devices.first(), battery)
                RfPacketParser.parseDeviceStatus(packet, rssi)?.let {
                    _deviceStatuses.emit(it)
                }
            }
        }
    }

    override suspend fun stopScanning() {
        scanningJob?.cancel()
        scanningJob = null
    }
}
