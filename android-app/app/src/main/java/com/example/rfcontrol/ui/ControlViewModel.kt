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
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ControlUiState(
    val deviceId: String = "111111",
    val companyIdText: String = "0000",
    val senderMac: String = RfPacketBuilder.DefaultSenderMac,
    val targetPduType: String = "ADV_NONCONN_IND",
    val targetPduHeader: String = "42 25",
    val selectedMode: ControlMode = ControlMode.Mode1,
    val levels: StrengthLevels = StrengthLevels(),
    val isAdvertising: Boolean = false,
    val bleCapability: String = "未检测",
    val txCount: Long = 0,
    val logs: List<EventLog> = emptyList()
) {
    val validDeviceId: Boolean
        get() = RfPacketBuilder.isDeviceIdValid(deviceId)

    val validCompanyId: Boolean
        get() = Regex("^[0-9A-F]{4}$").matches(companyIdText)

    val companyIdValue: Int
        get() = companyIdText.toIntOrNull(16) ?: 0

    val validInputs: Boolean
        get() = validDeviceId && validCompanyId

    val advertisingButtonLabel: String
        get() = if (isAdvertising) "停止冲浪" else "开始冲浪"

    val txPacket: ByteArray
        get() = RfPacketBuilder.buildControlPacket(deviceId, selectedMode, levels, senderMac, companyIdValue)

    val txAdvData: ByteArray
        get() = RfPacketBuilder.buildControlAdvData(deviceId, selectedMode, levels, senderMac, companyIdValue)

    val txPacketHex: String
        get() = RfPacketBuilder.toHex(txPacket)

    val txAdvDataHex: String
        get() = RfPacketBuilder.toHex(txAdvData)

    val txNameAdHex: String
        get() = RfPacketBuilder.toHex(txAdvData.sliceArray(0..9))

    val txManufacturerAdHex: String
        get() = RfPacketBuilder.toHex(txAdvData.sliceArray(10..30))
}

class ControlViewModel : ViewModel() {
    private var realBleTransport: RfTransport? = null
    private var txTimerJob: Job? = null
    private var refreshAdvertisingJob: Job? = null
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
                    _uiState.update {
                        it.copy(
                            isAdvertising = false,
                            logs = prependLog(it.logs, LogType.Error, event.message)
                        )
                    }
                }
            }
        }
    }

    fun noteBlePermissionResult(granted: Boolean) {
        if (!granted) {
            stopTxTimer()
            _uiState.update {
                it.copy(
                    isAdvertising = false,
                    logs = prependLog(it.logs, LogType.Error, "蓝牙广播权限未授权")
                )
            }
        }
    }

    fun updateDeviceId(value: String) {
        val normalized = value
            .uppercase(Locale.US)
            .filter { it in '0'..'9' || it in 'A'..'Z' }
            .take(6)
        _uiState.update { it.copy(deviceId = normalized) }
        refreshAdvertisingIfRunning()
    }

    fun updateCompanyId(value: String) {
        val normalized = value
            .uppercase(Locale.US)
            .filter { it in '0'..'9' || it in 'A'..'F' }
            .take(4)
        _uiState.update { it.copy(companyIdText = normalized) }
        refreshAdvertisingIfRunning()
    }

    fun selectMode(mode: ControlMode) {
        _uiState.update { it.copy(selectedMode = mode) }
        refreshAdvertisingIfRunning()
    }

    fun updateVibration(value: Int) = updateLevels { it.copy(vibration = value.coerceIn(0, 100)) }
    fun updateSlap(value: Int) = updateLevels { it.copy(slap = value.coerceIn(0, 100)) }
    fun updateSuction(value: Int) = updateLevels { it.copy(suction = value.coerceIn(0, 100)) }
    fun updateClip(value: Int) = updateLevels { it.copy(clip = value.coerceIn(0, 6)) }
    fun updateElectric(value: Int) = updateLevels { it.copy(electric = value.coerceIn(0, 100)) }
    fun updateHeat(value: Int) = updateLevels { it.copy(heat = value.coerceIn(0, 100)) }

    fun startAdvertising() {
        val transport = realBleTransport ?: return
        val state = _uiState.value
        if (!state.validInputs) return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isAdvertising = true, txCount = 0, logs = emptyList()) }
                transport.startAdvertising { _uiState.value.txPacket }
                startOneSecondTimer()
            } catch (error: Throwable) {
                stopTxTimer()
                _uiState.update {
                    it.copy(
                        isAdvertising = false,
                        logs = prependLog(it.logs, LogType.Error, error.message ?: "广播启动失败")
                    )
                }
            }
        }
    }

    fun stopAdvertising() {
        stopTxTimer()
        refreshAdvertisingJob?.cancel()
        refreshAdvertisingJob = null
        viewModelScope.launch {
            realBleTransport?.stopAdvertising()
        }
        _uiState.update { it.copy(isAdvertising = false) }
    }

    private fun updateLevels(transform: (StrengthLevels) -> StrengthLevels) {
        _uiState.update { it.copy(levels = transform(it.levels)) }
        refreshAdvertisingIfRunning()
    }

    private fun refreshAdvertisingIfRunning() {
        val transport = realBleTransport ?: return
        val state = _uiState.value
        if (!state.isAdvertising || !state.validInputs) return
        _uiState.update { current ->
            current.copy(
                txCount = current.txCount + TxCadence.ChangeBurstPacketCount,
                logs = prependTxLog(current.logs, burstLogMessage(current))
            )
        }
        refreshAdvertisingJob?.cancel()
        refreshAdvertisingJob = viewModelScope.launch {
            delay(250)
            try {
                transport.startAdvertising { _uiState.value.txPacket }
            } catch (error: Throwable) {
                stopTxTimer()
                _uiState.update {
                    it.copy(
                        isAdvertising = false,
                        logs = prependLog(it.logs, LogType.Error, error.message ?: "广播刷新失败")
                    )
                }
            }
        }
    }

    private fun startOneSecondTimer() {
        stopTxTimer()
        txTimerJob = viewModelScope.launch {
            while (true) {
                delay(TxCadence.SteadyIntervalMillis)
                _uiState.update {
                    it.copy(
                        txCount = it.txCount + 1,
                        logs = prependTxLog(it.logs, txLogMessage(it))
                    )
                }
            }
        }
    }

    private fun stopTxTimer() {
        txTimerJob?.cancel()
        txTimerJob = null
    }

    private fun prependTxLog(logs: List<EventLog>, message: String): List<EventLog> {
        return prependLog(logs, LogType.Tx, message)
    }

    private fun prependLog(logs: List<EventLog>, type: LogType, message: String): List<EventLog> {
        return (listOf(EventLog(type, "${now()}  $message")) + logs).take(1)
    }

    private fun txLogMessage(state: ControlUiState): String {
        return "TYPE=${state.targetPduType} NameAd=${state.txNameAdHex} ManufacturerAd=${state.txManufacturerAdHex} AdvData=${state.txAdvDataHex}"
    }

    private fun burstLogMessage(state: ControlUiState): String {
        return "BURST=${TxCadence.ChangeBurstPacketCount} ${txLogMessage(state)}"
    }

    private fun now(): String = LocalTime.now().format(timeFormatter)
}
