package com.example.rfcontrol.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rfcontrol.data.protocol.ControlMode
import com.example.rfcontrol.data.protocol.EventLog
import com.example.rfcontrol.data.protocol.LogType
import com.example.rfcontrol.data.protocol.RfPacketBuilder
import com.example.rfcontrol.data.protocol.StrengthLevels
import com.example.rfcontrol.data.transport.RfTransport
import com.example.rfcontrol.data.transport.TransportEventType
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ControlUiState(
    val deviceId: String = "111111",
    val senderMac: String = RfPacketBuilder.DefaultSenderMac,
    val broadcastUuid: String = "0000FFF0-0000-1000-8000-00805F9B34FB",
    val selectedMode: ControlMode = ControlMode.Mode1,
    val levels: StrengthLevels = StrengthLevels(),
    val isAdvertising: Boolean = false,
    val bleCapability: String = "未检测",
    val txCount: Long = 0,
    val logs: List<EventLog> = emptyList()
) {
    val validDeviceId: Boolean
        get() = RfPacketBuilder.isDeviceIdValid(deviceId)

    val txPacket: ByteArray
        get() = RfPacketBuilder.buildControlPacket(deviceId, selectedMode, levels, senderMac)
}

class ControlViewModel : ViewModel() {
    private var realBleTransport: RfTransport? = null
    private var txTimerJob: Job? = null
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    fun configureRealBleTransport(transport: RfTransport, capabilitySummary: String) {
        realBleTransport = transport
        _uiState.update { it.copy(bleCapability = capabilitySummary) }
        viewModelScope.launch {
            transport.transportEvents.collect { event ->
                if (event.type == TransportEventType.Tx && event.message.isNotBlank()) {
                    _uiState.update {
                        it.copy(
                            txCount = it.txCount + 1,
                            logs = prependTxLog(it.logs, event.message)
                        )
                    }
                } else if (event.type == TransportEventType.Error) {
                    stopTxTimer()
                    _uiState.update { it.copy(isAdvertising = false) }
                }
            }
        }
    }

    fun noteBlePermissionResult(granted: Boolean) {
        if (!granted) {
            stopTxTimer()
            _uiState.update { it.copy(isAdvertising = false, logs = emptyList()) }
        }
    }

    fun updateDeviceId(value: String) {
        _uiState.update { it.copy(deviceId = value.uppercase().take(8)) }
    }

    fun startAdvertising() {
        val transport = realBleTransport ?: return
        val state = _uiState.value
        if (!state.validDeviceId) return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isAdvertising = true, txCount = 0, logs = emptyList()) }
                transport.startAdvertising { _uiState.value.txPacket }
                startOneSecondTimer(transport)
            } catch (error: Throwable) {
                stopTxTimer()
                _uiState.update { it.copy(isAdvertising = false, logs = emptyList()) }
            }
        }
    }

    fun stopAdvertising() {
        stopTxTimer()
        viewModelScope.launch {
            realBleTransport?.stopAdvertising()
        }
        _uiState.update { it.copy(isAdvertising = false) }
    }

    private fun startOneSecondTimer(transport: RfTransport) {
        stopTxTimer()
        txTimerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                try {
                    transport.startAdvertising { _uiState.value.txPacket }
                } catch (error: Throwable) {
                    stopTxTimer()
                    _uiState.update { it.copy(isAdvertising = false) }
                }
            }
        }
    }

    private fun stopTxTimer() {
        txTimerJob?.cancel()
        txTimerJob = null
    }

    private fun prependTxLog(logs: List<EventLog>, message: String): List<EventLog> {
        return (listOf(EventLog(LogType.Tx, "${now()}  $message")) + logs).take(8)
    }

    private fun now(): String = LocalTime.now().format(timeFormatter)
}
