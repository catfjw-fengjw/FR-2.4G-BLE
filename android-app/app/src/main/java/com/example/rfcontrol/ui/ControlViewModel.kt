package com.example.rfcontrol.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rfcontrol.data.protocol.ControlMode
import com.example.rfcontrol.data.protocol.EventLog
import com.example.rfcontrol.data.protocol.LogType
import com.example.rfcontrol.data.protocol.RfDevice
import com.example.rfcontrol.data.protocol.RfPacketBuilder
import com.example.rfcontrol.data.protocol.StrengthLevels
import com.example.rfcontrol.data.transport.MockRfTransport
import com.example.rfcontrol.data.transport.RfTransport
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ControlUiState(
    val activeTab: AppTab = AppTab.Console,
    val deviceId: String = "LX_DX001",
    val selectedMode: ControlMode = ControlMode.Mode1,
    val levels: StrengthLevels = StrengthLevels(),
    val isAdvertising: Boolean = false,
    val isScanning: Boolean = true,
    val battery: Int = 86,
    val rssi: Int = -46,
    val lastRxAt: String = "等待收包",
    val txCount: Long = 0,
    val logs: List<EventLog> = listOf(
        EventLog(LogType.Info, "原型已加载，当前使用模拟广播链路。"),
        EventLog(LogType.Warn, "42 字节为空中包预览；真机广播使用 Byte9~Byte39 的 31 字节 AdvData。")
    )
) {
    val validDeviceId: Boolean
        get() = RfPacketBuilder.isDeviceIdValid(deviceId)

    val txPacket: ByteArray
        get() = RfPacketBuilder.buildControlPacket(deviceId, selectedMode, levels)

    val txPacketHex: String
        get() = RfPacketBuilder.toHex(txPacket)
}

enum class AppTab(val label: String) {
    Console("控制台"),
    Devices("设备扫描"),
    Debug("协议调试")
}

class ControlViewModel(
    private val transport: RfTransport = MockRfTransport()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    val scannedDevices: StateFlow<List<RfDevice>> = transport.scannedDevices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = listOf(
            RfDevice("LX_DX001", "01:01:01:00:00:01", -46, 86, "刚刚"),
            RfDevice("LX_DX014", "01:01:02:00:00:0E", -63, 72, "4 秒前"),
            RfDevice("LX_DX108", "01:03:08:00:00:6C", -71, 48, "12 秒前")
        )
    )

    private var txCounterJob: Job? = null
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    init {
        viewModelScope.launch {
            transport.startScanning()
        }
        viewModelScope.launch {
            transport.deviceStatuses.collect { status ->
                _uiState.update {
                    it.copy(
                        battery = status.battery,
                        rssi = status.rssi,
                        lastRxAt = now(),
                        logs = prependLog(it.logs, LogType.Rx, "收到设备端状态包，电量 ${status.battery}%，RSSI ${status.rssi} dBm。")
                    )
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun updateDeviceId(value: String) {
        _uiState.update { it.copy(deviceId = value.uppercase().take(16)) }
    }

    fun selectMode(mode: ControlMode) {
        _uiState.update {
            val previewPacket = RfPacketBuilder.buildControlPacket(it.deviceId, mode, it.levels)
            it.copy(
                selectedMode = mode,
                logs = prependLog(
                    it.logs,
                    LogType.Tx,
                    "选择 ${mode.label}，Byte22=${RfPacketBuilder.byteHex(mode.value)}，Byte23=${RfPacketBuilder.byteHex(previewPacket[22].toInt())}。"
                )
            )
        }
    }

    fun updateStrength(field: StrengthField, value: Int) {
        _uiState.update {
            val nextLevels = when (field) {
                StrengthField.Vibration -> it.levels.copy(vibration = value.coerceIn(0, 100))
                StrengthField.Slap -> it.levels.copy(slap = value.coerceIn(0, 100))
                StrengthField.Suction -> it.levels.copy(suction = value.coerceIn(0, 100))
                StrengthField.Clip -> it.levels.copy(clip = value.coerceIn(0, 6))
                StrengthField.Electric -> it.levels.copy(electric = value.coerceIn(0, 100))
                StrengthField.Heat -> it.levels.copy(heat = value.coerceIn(0, 100))
            }
            it.copy(levels = nextLevels)
        }
    }

    fun startAdvertising() {
        val state = _uiState.value
        if (!state.validDeviceId) {
            addLog(LogType.Error, "设备 ID 格式错误，示例格式：LX_DX001。")
            return
        }
        _uiState.update {
            it.copy(
                isAdvertising = true,
                logs = prependLog(it.logs, LogType.Ok, "开始模拟广播，目标间隔 10ms。")
            )
        }
        viewModelScope.launch {
            transport.startAdvertising { _uiState.value.txPacket }
        }
        txCounterJob?.cancel()
        txCounterJob = viewModelScope.launch {
            while (true) {
                delay(10)
                _uiState.update { current ->
                    val nextCount = current.txCount + 1
                    val nextLogs = if (nextCount % 100L == 0L) {
                        prependLog(current.logs, LogType.Tx, "持续广播中，累计发送 $nextCount 包。")
                    } else {
                        current.logs
                    }
                    current.copy(txCount = nextCount, logs = nextLogs)
                }
            }
        }
    }

    fun stopAdvertising() {
        txCounterJob?.cancel()
        txCounterJob = null
        viewModelScope.launch {
            transport.stopAdvertising()
        }
        _uiState.update {
            it.copy(
                isAdvertising = false,
                logs = prependLog(it.logs, LogType.Info, "已停止模拟广播。")
            )
        }
    }

    fun setScanning(enabled: Boolean) {
        _uiState.update { it.copy(isScanning = enabled) }
        viewModelScope.launch {
            if (enabled) {
                transport.startScanning()
                addLog(LogType.Ok, "开始模拟扫描。")
            } else {
                transport.stopScanning()
                addLog(LogType.Info, "已暂停模拟扫描。")
            }
        }
    }

    fun bindDevice(device: RfDevice) {
        _uiState.update {
            it.copy(
                activeTab = AppTab.Console,
                deviceId = device.id,
                battery = device.battery,
                rssi = device.rssi,
                lastRxAt = "刚刚",
                logs = prependLog(it.logs, LogType.Ok, "已绑定设备 ${device.id} / ${device.mac}。")
            )
        }
    }

    fun manualBindDevice() {
        val state = _uiState.value
        if (!state.validDeviceId) {
            addLog(LogType.Error, "手动绑定失败：设备 ID 格式错误。")
            return
        }
        bindDevice(RfDevice(state.deviceId, "00:00:00:00:00:00", -58, 80, "手动"))
    }

    fun simulateSend() {
        addLog(LogType.Tx, "手动模拟发送：${_uiState.value.txPacketHex}")
    }

    fun simulateReceive() {
        _uiState.update {
            val nextBattery = (it.battery + listOf(-1, 1).random()).coerceIn(30, 100)
            val nextRssi = (it.rssi + (-3..3).random()).coerceIn(-82, -35)
            it.copy(
                battery = nextBattery,
                rssi = nextRssi,
                lastRxAt = now(),
                logs = prependLog(it.logs, LogType.Rx, "收到设备端状态包，电量 $nextBattery%，RSSI $nextRssi dBm。")
            )
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    fun noteCopied() {
        addLog(LogType.Ok, "已复制当前 42 字节空中包十六进制预览。")
    }

    private fun addLog(type: LogType, message: String) {
        _uiState.update { it.copy(logs = prependLog(it.logs, type, message)) }
    }

    private fun prependLog(logs: List<EventLog>, type: LogType, message: String): List<EventLog> {
        return (listOf(EventLog(type, "${now()}  $message")) + logs).take(80)
    }

    private fun now(): String = LocalTime.now().format(timeFormatter)
}

enum class StrengthField(val label: String, val byteNumber: Int, val max: Int) {
    Vibration("震动强度", 24, 100),
    Slap("拍打强度", 25, 100),
    Suction("吮吸强度", 26, 100),
    Electric("电击强度", 28, 100),
    Heat("加热强度", 29, 100),
    Clip("夹吸档位", 27, 6)
}
