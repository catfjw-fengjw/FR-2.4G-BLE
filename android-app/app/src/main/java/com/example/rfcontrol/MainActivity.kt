package com.example.rfcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfcontrol.data.protocol.ControlMode
import com.example.rfcontrol.data.protocol.EventLog
import com.example.rfcontrol.data.protocol.LogType
import com.example.rfcontrol.data.protocol.RfDevice
import com.example.rfcontrol.data.protocol.RfPacketBuilder
import com.example.rfcontrol.ui.AppTab
import com.example.rfcontrol.ui.ControlUiState
import com.example.rfcontrol.ui.ControlViewModel
import com.example.rfcontrol.ui.StrengthField
import com.example.rfcontrol.ui.theme.RfAmber
import com.example.rfcontrol.ui.theme.RfBg
import com.example.rfcontrol.ui.theme.RfBlue
import com.example.rfcontrol.ui.theme.RfControlTheme
import com.example.rfcontrol.ui.theme.RfGreen
import com.example.rfcontrol.ui.theme.RfLine
import com.example.rfcontrol.ui.theme.RfLineSoft
import com.example.rfcontrol.ui.theme.RfMuted
import com.example.rfcontrol.ui.theme.RfPanel
import com.example.rfcontrol.ui.theme.RfPanelStrong
import com.example.rfcontrol.ui.theme.RfRed
import com.example.rfcontrol.ui.theme.RfSubtle
import com.example.rfcontrol.ui.theme.RfTeal
import com.example.rfcontrol.ui.theme.RfText

class MainActivity : ComponentActivity() {
    private val controlViewModel by viewModels<ControlViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RfControlTheme {
                val state by controlViewModel.uiState.collectAsState()
                val devices by controlViewModel.scannedDevices.collectAsState()
                RfControlApp(state, devices, controlViewModel)
            }
        }
    }
}

@Composable
private fun RfControlApp(
    state: ControlUiState,
    devices: List<RfDevice>,
    viewModel: ControlViewModel
) {
    Scaffold(
        containerColor = RfBg,
        topBar = {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    text = "RF2.4G PSEUDO BLE CONTROL PROTOTYPE",
                    color = RfTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RF2.4G 控制端", color = RfText, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill("扫描中", state.isScanning)
                        StatusPill(if (state.isAdvertising) "广播中" else "广播停止", state.isAdvertising)
                    }
                }
                Spacer(Modifier.height(12.dp))
                TabBar(state.activeTab, viewModel::selectTab)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(RfBg)
        ) {
            when (state.activeTab) {
                AppTab.Console -> ConsoleScreen(state, viewModel)
                AppTab.Devices -> DevicesScreen(state, devices, viewModel)
                AppTab.Debug -> DebugScreen(state, viewModel)
            }
        }
    }
}

@Composable
private fun TabBar(activeTab: AppTab, onSelect: (AppTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF151513))
            .border(1.dp, RfLine, RoundedCornerShape(8.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AppTab.entries.forEach { tab ->
            TextButton(
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (activeTab == tab) RfPanelStrong else Color.Transparent,
                    contentColor = if (activeTab == tab) RfText else RfMuted
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(tab.label, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ConsoleScreen(state: ControlUiState, viewModel: ControlViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { DeviceStatusPanel(state, viewModel) }
        item { ModePanel(state.selectedMode, viewModel::selectMode) }
        item { StrengthPanel(state, viewModel) }
        item { SendPanel(state, viewModel) }
        item { PacketPreview(state) }
        item { LogPanel(state.logs.take(12)) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun DeviceStatusPanel(state: ControlUiState, viewModel: ControlViewModel) {
    Panel {
        OutlinedTextField(
            value = state.deviceId,
            onValueChange = viewModel::updateDeviceId,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("目标设备 ID") },
            supportingText = {
                Text(
                    text = if (state.validDeviceId) "格式有效" else "格式错误，示例：LX_DX001",
                    color = if (state.validDeviceId) RfTeal else RfRed,
                    fontWeight = FontWeight.Bold
                )
            },
            singleLine = true
        )
        Spacer(Modifier.height(10.dp))
        MetricGrid(
            listOf(
                "电量" to "${state.battery}%",
                "RSSI" to "${state.rssi} dBm",
                "最近收包" to state.lastRxAt,
                "发送计数" to state.txCount.toString()
            )
        )
    }
}

@Composable
private fun ModePanel(selectedMode: ControlMode, onSelect: (ControlMode) -> Unit) {
    Panel(title = "模式控制", subtitle = "Byte22 模式字段") {
        ControlMode.entries.take(9).chunked(3).forEach { rowModes ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowModes.forEach { mode ->
                    ModeButton(mode, selectedMode == mode, Modifier.weight(1f), onSelect)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        ControlMode.entries.drop(9).chunked(2).forEach { rowModes ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowModes.forEach { mode ->
                    ModeButton(mode, selectedMode == mode, Modifier.weight(1f), onSelect)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ModeButton(
    mode: ControlMode,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: (ControlMode) -> Unit
) {
    val borderColor = when {
        selected && mode == ControlMode.PowerOff -> RfRed
        selected && mode == ControlMode.HeatOn -> RfAmber
        selected -> RfTeal
        else -> RfLine
    }
    OutlinedButton(
        onClick = { onSelect(mode) },
        modifier = modifier.height(68.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) borderColor.copy(alpha = 0.16f) else Color(0xFF151512),
            contentColor = if (selected) borderColor else RfText
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(mode.label, fontWeight = FontWeight.Black)
            Text(RfPacketBuilder.byteHex(mode.value), color = RfMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun StrengthPanel(state: ControlUiState, viewModel: ControlViewModel) {
    Panel(title = "强度控制", subtitle = "Byte24~Byte29 控制字段") {
        StrengthSlider(StrengthField.Vibration, state.levels.vibration, viewModel)
        StrengthSlider(StrengthField.Slap, state.levels.slap, viewModel)
        StrengthSlider(StrengthField.Suction, state.levels.suction, viewModel)
        StrengthSlider(StrengthField.Electric, state.levels.electric, viewModel)
        StrengthSlider(StrengthField.Heat, state.levels.heat, viewModel)
        ClipSelector(state.levels.clip, viewModel)
    }
}

@Composable
private fun StrengthSlider(field: StrengthField, value: Int, viewModel: ControlViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF151512))
            .border(1.dp, RfLineSoft, RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(field.label, color = RfMuted, fontSize = 13.sp)
            Text("Byte${field.byteNumber} = ${RfPacketBuilder.byteHex(value)}", color = RfMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = value.toFloat(),
                onValueChange = { viewModel.updateStrength(field, it.toInt()) },
                valueRange = 0f..field.max.toFloat(),
                modifier = Modifier.weight(1f)
            )
            Text(value.toString(), color = RfTeal, fontWeight = FontWeight.Black, modifier = Modifier.width(42.dp))
        }
    }
}

@Composable
private fun ClipSelector(value: Int, viewModel: ControlViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF151512))
            .border(1.dp, RfLineSoft, RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("夹吸档位", color = RfMuted, fontSize = 13.sp)
            Text("Byte27 = ${RfPacketBuilder.byteHex(value)}", color = RfMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..6).forEach { index ->
                OutlinedButton(
                    onClick = { viewModel.updateStrength(StrengthField.Clip, index) },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, if (value == index) RfTeal else RfLine),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (value == index) RfTeal.copy(alpha = 0.16f) else Color(0xFF151512),
                        contentColor = if (value == index) RfTeal else RfText
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(index.toString(), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun SendPanel(state: ControlUiState, viewModel: ControlViewModel) {
    Panel(title = "广播发送", subtitle = "模拟 10ms 重复发送") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { if (state.isAdvertising) viewModel.stopAdvertising() else viewModel.startAdvertising() },
                colors = ButtonDefaults.buttonColors(containerColor = RfTeal, contentColor = Color(0xFF07110E))
            ) {
                Text(if (state.isAdvertising) "停止广播" else "开始广播", fontWeight = FontWeight.Black)
            }
            CopyPacketButton(state, viewModel)
        }
        Spacer(Modifier.height(10.dp))
        Text("真机广播使用 Byte9~Byte39 的 31 字节 AdvData；头/MAC/CRC 由链路层处理。", color = RfAmber, fontSize = 13.sp)
    }
}

@Composable
private fun CopyPacketButton(state: ControlUiState, viewModel: ControlViewModel) {
    val clipboard = LocalClipboardManager.current
    OutlinedButton(
        onClick = {
            clipboard.setText(AnnotatedString(state.txPacketHex))
            viewModel.noteCopied()
        },
        border = BorderStroke(1.dp, RfLine),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = RfText)
    ) {
        Text("复制包", fontWeight = FontWeight.Black)
    }
}

@Composable
private fun PacketPreview(state: ControlUiState) {
    Panel(title = "发送包预览", subtitle = "42 字节空中包 HEX") {
        val bytes = state.txPacket
        bytes.toList().chunked(7).forEachIndexed { index, group ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF11110F))
                    .border(1.dp, RfLineSoft, RoundedCornerShape(6.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${index * 7 + 1}".padStart(2, '0'), color = RfSubtle, fontSize = 12.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    group.joinToString(" ") { RfPacketBuilder.byteHex(it.toInt()).removePrefix("0x") },
                    color = RfText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldChip("Byte22 ${RfPacketBuilder.byteHex(bytes[21].toInt())}")
            FieldChip("Byte23 ${RfPacketBuilder.byteHex(bytes[22].toInt())}")
        }
        Spacer(Modifier.height(8.dp))
        SelectionContainer {
            Text(
                state.txPacketHex,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F0F0D))
                    .border(1.dp, RfLine, RoundedCornerShape(6.dp))
                    .padding(10.dp),
                color = RfMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun DevicesScreen(state: ControlUiState, devices: List<RfDevice>, viewModel: ControlViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Panel(title = "模拟设备扫描", subtitle = "设备端每 5s 状态广播") {
                Button(
                    onClick = { viewModel.setScanning(!state.isScanning) },
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.isScanning) RfTeal else RfPanelStrong)
                ) {
                    Text(if (state.isScanning) "暂停扫描" else "开始扫描", fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(10.dp))
                Text("列表数据为原型模拟，用于确认绑定流程。", color = RfMuted, fontSize = 13.sp)
            }
        }
        items(devices) { device ->
            DeviceRow(device, selected = device.id == state.deviceId, onClick = { viewModel.bindDevice(device) })
        }
        item {
            Panel(title = "手动绑定", subtitle = "设备 ID 作为核心匹配条件") {
                OutlinedTextField(
                    value = state.deviceId,
                    onValueChange = viewModel::updateDeviceId,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("设备 ID") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (state.validDeviceId) "格式有效，可绑定。" else "请输入 LX_DX001 这类 8 位 ID。",
                    color = if (state.validDeviceId) RfTeal else RfRed,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    enabled = state.validDeviceId,
                    onClick = viewModel::manualBindDevice,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RfTeal, contentColor = Color(0xFF07110E))
                ) {
                    Text("绑定手动设备", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: RfDevice, selected: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp),
        border = BorderStroke(1.dp, if (selected) RfTeal else RfLine),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) RfTeal.copy(alpha = 0.16f) else Color(0xFF151512),
            contentColor = RfText
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(device.id, fontWeight = FontWeight.Black)
                Text(device.mac, color = RfMuted, fontSize = 12.sp)
            }
            Text("${device.rssi} dBm", color = RfAmber)
            Spacer(Modifier.width(16.dp))
            Text("${device.battery}%", color = RfGreen)
        }
    }
}

@Composable
private fun DebugScreen(state: ControlUiState, viewModel: ControlViewModel) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Panel(title = "协议字段表", subtitle = "Byte1~Byte42 当前发送包") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::simulateSend, colors = ButtonDefaults.buttonColors(containerColor = RfTeal)) {
                    Text("模拟发送", fontWeight = FontWeight.Black)
                }
                OutlinedButton(onClick = viewModel::simulateReceive, border = BorderStroke(1.dp, RfLine)) {
                    Text("模拟接收", color = RfText)
                }
                CopyPacketButton(state, viewModel)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = viewModel::clearLogs, border = BorderStroke(1.dp, RfLine)) {
                Text("清空日志", color = RfMuted)
            }
            Spacer(Modifier.height(12.dp))
            FieldTable(state.txPacket)
        }
        Panel(title = "校验信息", subtitle = "协议层校验结果") {
            MetricGrid(
                listOf(
                    "APP 校验码" to RfPacketBuilder.byteHex(state.txPacket[22].toInt()),
                    "设备校验示例" to "MAC & 0xFF",
                    "CRC 状态" to "链路层处理"
                )
            )
            Spacer(Modifier.height(10.dp))
            SelectionContainer {
                Text(state.txPacketHex, color = RfMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
        LogPanel(state.logs)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun FieldTable(packet: ByteArray) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RfLine, RoundedCornerShape(6.dp))
    ) {
        FieldRow("字节", "值", "字段", header = true)
        packet.forEachIndexed { index, value ->
            val byteNumber = index + 1
            FieldRow(
                "Byte$byteNumber",
                RfPacketBuilder.byteHex(value.toInt()),
                fieldName(byteNumber),
                highlight = byteNumber == 22 || byteNumber == 23 || byteNumber in 24..29 || byteNumber >= 40
            )
        }
    }
}

@Composable
private fun FieldRow(byte: String, value: String, name: String, header: Boolean = false, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    header -> Color(0xFF11110F)
                    highlight -> RfAmber.copy(alpha = 0.08f)
                    else -> Color.Transparent
                }
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(byte, color = if (header) RfMuted else RfText, modifier = Modifier.width(74.dp), fontSize = 12.sp)
        Text(value, color = RfText, modifier = Modifier.width(76.dp), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Text(name, color = if (header) RfMuted else RfMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MetricGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF151512))
                            .border(1.dp, RfLineSoft, RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Text(label, color = RfMuted, fontSize = 12.sp)
                        Text(value, color = metricColor(label), fontSize = 17.sp, fontWeight = FontWeight.Black)
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LogPanel(logs: List<EventLog>) {
    Panel(title = "事件日志", subtitle = "发送 / 接收 / 校验 / 异常") {
        if (logs.isEmpty()) {
            Text("暂无日志", color = RfMuted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                logs.forEach { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF11110F))
                            .border(1.dp, RfLineSoft, RoundedCornerShape(6.dp))
                            .padding(9.dp)
                    ) {
                        Text(log.type.name.uppercase(), color = logColor(log.type), modifier = Modifier.width(48.dp), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text(log.message, color = RfMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, active: Boolean) {
    Surface(
        color = if (active) RfTeal.copy(alpha = 0.16f) else Color(0xFF181815),
        contentColor = if (active) RfTeal else RfMuted,
        border = BorderStroke(1.dp, if (active) RfTeal.copy(alpha = 0.55f) else RfLine),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp)
    }
}

@Composable
private fun Panel(
    title: String? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RfPanel)
            .border(1.dp, RfLine, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        if (title != null) {
            Text(title, color = RfText, fontSize = 18.sp, fontWeight = FontWeight.Black)
            if (subtitle != null) Text(subtitle, color = RfMuted, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
        }
        content()
    }
}

@Composable
private fun FieldChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(RfAmber.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(text, color = RfAmber, fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}

private fun fieldName(byte: Int): String = when (byte) {
    1 -> "广播类型"
    2 -> "有效数据长度"
    in 3..8 -> "控制端 MAC"
    in 9..11 -> "BLE Flags"
    12 -> "名称段长度"
    13 -> "名称字段类型"
    in 14..21 -> "设备端 ID"
    22 -> "模式定义"
    23 -> "APP 校验码"
    24 -> "震动强度"
    25 -> "拍打强度"
    26 -> "吮吸强度"
    27 -> "夹吸档位"
    28 -> "电击强度"
    29 -> "加热强度"
    in 30..39 -> "保留"
    else -> "链路层 CRC"
}

private fun metricColor(label: String): Color = when (label) {
    "电量" -> RfGreen
    "RSSI" -> RfAmber
    "CRC 状态" -> RfRed
    else -> RfText
}

private fun logColor(type: LogType): Color = when (type) {
    LogType.Ok, LogType.Rx -> RfTeal
    LogType.Tx -> RfBlue
    LogType.Warn -> RfAmber
    LogType.Error -> RfRed
    LogType.Info -> RfSubtle
}
