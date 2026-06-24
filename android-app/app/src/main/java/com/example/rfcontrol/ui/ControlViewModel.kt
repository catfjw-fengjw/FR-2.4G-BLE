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
import com.example.rfcontrol.data.transport.TransportEvent
import com.example.rfcontrol.data.transport.TransportEventType
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
    val deviceId: String = "111111",
    val selectedMode: ControlMode = ControlMode.Mode1,
    val levels: StrengthLevels = StrengthLevels(),
    val isAdvertising: Boolean = false,
    val isScanning: Boolean = false,
    val battery: Int = 86,
    val rssi: Int = -46,
    val lastRxAt: String = "等待收包",
    val txCount: Long = 0,
    val useRealBle: Boolean = true,
    val bleCapability: String = "未检测",
    val logs: List<EventLog> = listOf(
        EventLog(LogType.Info, "控制台已加载，默认设备 ID：111111。"),
        EventLog(LogType.Warn, "真机广播使用 Manufacturer Data 承载核心协议字段。")
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
    private val mockTransport: RfTransport = MockRfTransport()
) : ViewModel() {
    private var realBleTransport: RfTransport? = null
    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    val scannedDevices: StateFlow<List<RfDevice>> = mockTransport.scannedDevices.stateIn(
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
            mockTransport.startScanning()
        }
        viewModelScope.launch {
            mockTransport.deviceStatuses.collect { status ->
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
        collectTransportEvents(mockTransport)
    }

    fun configureRealBleTransport(transport: RfTransport, capabilitySummary: String) {
        realBleTransport = transport
        _uiState.update {
            it.copy(
                bleCapability = capabilitySummary,
                logs = prependLog(it.logs, LogType.Info, "真机 BLE 能力检测：$capabilitySummary。")
            )
        }
        collectTransportEvents(transport)
    }

    fun setUseRealBle(enabled: Boolean) {
        _uiState.update {
            it.copy(
                useRealBle = enabled,
                logs = prependLog(
                    it.logs,
                    if (enabled) LogType.Warn else LogType.Info,
                    if (enabled) {
                        "已切换为真机 BLE 调试广播。注意：Android API 使用 Manufacturer Data 承载核心字段。"
                    } else {
                        "已切换为模拟广播链路。"
                    }
                )
            )
        }
    }

    fun noteBlePermissionResult(granted: Boolean) {
        addLog(
            if (granted) LogType.Ok else LogType.Error,
            if (granted) "蓝牙运行时权限已授权。" else "蓝牙运行时权限未全部授权，真机广播可能无法启动。"
        )
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun updateDeviceId(value: String) {
        _uiState.update { it.copy(deviceId = value.uppercase().take(8)) }
        refreshRealAdvertisementIfRunning("设备 ID 更新")
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
        refreshRealAdvertisementIfRunning("模式 ${mode.label}")
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
        refreshRealAdvertisementIfRunning("${field.label}=$value")
    }

    fun startAdvertising() {
        val state = _uiState.value
        if (!state.validDeviceId) {
            addLog(LogType.Error, "设备 ID 格式错误，请输入 1~8 位数字或大写字母。")
            return
        }
        val activeTransport = activeTransport()
        val channelName = if (state.useRealBle) "真机 BLE 调试广播" else "模拟广播"
        viewModelScope.launch {
            try {
                activeTransport.startAdvertising { _uiState.value.txPacket }
                _uiState.update {
                    it.copy(
                        isAdvertising = true,
                        logs = prependLog(it.logs, LogType.Ok, "开始$channelName，界面计数按 10ms 刷新。")
                    )
                }
                startTxCounter(channelName)
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isAdvertising = false,
                        logs = prependLog(it.logs, LogType.Error, "$channelName 启动失败：${error.message ?: "未知错误"}。")
                    )
                }
            }
        }
    }

    private fun startTxCounter(channelName: String) {
        txCounterJob?.cancel()
        txCounterJob = viewModelScope.launch {
            while (true) {
                delay(10)
                _uiState.update { current ->
                    val nextCount = current.txCount + 1
                    val nextLogs = if (nextCount % 100L == 0L) {
                        prependLog(current.logs, LogType.Tx, "$channelName 持续运行，累计刷新 $nextCount 次。")
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
            activeTransport().stopAdvertising()
        }
        _uiState.update {
            it.copy(
                isAdvertising = false,
                logs = prependLog(it.logs, LogType.Info, "已停止广播。")
            )
        }
    }

    fun setScanning(enabled: Boolean) {
        _uiState.update { it.copy(isScanning = enabled) }
        viewModelScope.launch {
            if (enabled) {
                mockTransport.startScanning()
                addLog(LogType.Ok, "开始模拟扫描。")
            } else {
                mockTransport.stopScanning()
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

    private fun activeTransport(): RfTransport {
        return if (_uiState.value.useRealBle) realBleTransport ?: mockTransport else mockTransport
    }

    private fun collectTransportEvents(transport: RfTransport) {
        viewModelScope.launch {
            transport.transportEvents.collect { event ->
                if (event.type == TransportEventType.Error) {
                    txCounterJob?.cancel()
                    txCounterJob = null
                    _uiState.update { it.copy(isAdvertising = false) }
                }
                addLog(event.toLogType(), event.message)
            }
        }
    }

    private fun refreshRealAdvertisementIfRunning(reason: String) {
        val state = _uiState.value
        if (!state.isAdvertising || !state.useRealBle || !state.validDeviceId) return
        val transport = realBleTransport ?: return
        viewModelScope.launch {
            try {
                transport.startAdvertising { _uiState.value.txPacket }
                addLog(LogType.Tx, "已刷新真机 BLE 调试广播：$reason。")
            } catch (error: Throwable) {
                txCounterJob?.cancel()
                txCounterJob = null
                _uiState.update { it.copy(isAdvertising = false) }
                addLog(LogType.Error, "刷新真机 BLE 广播失败：${error.message ?: "未知错误"}。")
            }
        }
    }

    private fun TransportEvent.toLogType(): LogType {
        return when (type) {
            TransportEventType.Info -> LogType.Info
            TransportEventType.Ok -> LogType.Ok
            TransportEventType.Warn -> LogType.Warn
            TransportEventType.Error -> LogType.Error
            TransportEventType.Tx -> LogType.Tx
            TransportEventType.Rx -> LogType.Rx
        }
    }
}

enum class StrengthField(val label: String, val byteNumber: Int, val max: Int) {
    Vibration("震动强度", 24, 100),
    Slap("拍打强度", 25, 100),
    Suction("吮吸强度", 26, 100),
    Electric("电击强度", 28, 100),
    Heat("加热强度", 29, 100),
    Clip("夹吸档位", 27, 6)
}
