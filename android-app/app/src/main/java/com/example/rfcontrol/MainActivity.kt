package com.example.rfcontrol

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfcontrol.data.protocol.ControlMode
import com.example.rfcontrol.data.protocol.EventLog
import com.example.rfcontrol.data.protocol.LogType
import com.example.rfcontrol.data.transport.BleDebugRfTransport
import com.example.rfcontrol.ui.ControlUiState
import com.example.rfcontrol.ui.ControlViewModel
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
    private val blePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        controlViewModel.noteBlePermissionResult(it.values.all { granted -> granted })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bleTransport = BleDebugRfTransport(this)
        controlViewModel.configureRealBleTransport(bleTransport, bleTransport.capabilitySummary())
        requestBlePermissionsIfNeeded()
        setContent {
            RfControlTheme {
                val state by controlViewModel.uiState.collectAsState()
                RfConsoleApp(state, controlViewModel)
            }
        }
    }

    private fun requestBlePermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        blePermissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        )
    }
}

@Composable
private fun RfConsoleApp(state: ControlUiState, viewModel: ControlViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("模式", "强度", "调试")

    Scaffold(containerColor = RfBg) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(RfBg)
        ) {
            AppHeader(state)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = RfPanel,
                contentColor = RfTeal
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Black else FontWeight.Bold
                            )
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(remember(selectedTab) { ScrollState(0) })
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (selectedTab) {
                    0 -> ModePage(state, viewModel)
                    1 -> StrengthPage(state, viewModel)
                    else -> DebugPage(state)
                }
            }
        }
    }
}

@Composable
private fun AppHeader(state: ControlUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "悦色 logo",
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    "悦色",
                    color = Color(0xFFE93578),
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black
                )
            }
            Text("懂你的“灵魂性”伴侣", color = RfMuted, fontSize = 12.sp)
        }
        StatusPill(if (state.isAdvertising) "广播中" else "已停止", state.isAdvertising)
    }
}

@Composable
private fun ModePage(state: ControlUiState, viewModel: ControlViewModel) {
    Panel(padding = 12) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.deviceId,
                onValueChange = viewModel::updateDeviceId,
                modifier = Modifier
                    .weight(1f)
                    .height(62.dp),
                label = { Text("设备码", fontSize = 10.sp) },
                textStyle = TextStyle(fontSize = 12.sp),
                isError = !state.validDeviceId,
                singleLine = true
            )
            OutlinedTextField(
                value = state.companyIdText,
                onValueChange = viewModel::updateCompanyId,
                modifier = Modifier
                    .weight(1f)
                    .height(62.dp),
                label = { Text("公司代码", fontSize = 10.sp) },
                textStyle = TextStyle(fontSize = 12.sp),
                isError = !state.validCompanyId,
                singleLine = true
            )
        }
    }

    Button(
        onClick = {
            if (state.isAdvertising) {
                viewModel.stopAdvertising()
            } else {
                viewModel.startAdvertising()
            }
        },
        enabled = state.isAdvertising || state.validInputs,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (state.isAdvertising) RfAmber else RfTeal,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(state.advertisingButtonLabel, fontWeight = FontWeight.Black, fontSize = 12.sp)
    }

    ModeSection(
        title = "模式选择",
        modes = ControlMode.values().take(9),
        selectedMode = state.selectedMode,
        onSelect = viewModel::selectMode
    )
    ModeSection(
        title = "加热控制",
        modes = listOf(ControlMode.HeatOn, ControlMode.HeatOff),
        selectedMode = state.selectedMode,
        onSelect = viewModel::selectMode
    )
    ModeSection(
        title = "设备控制",
        modes = listOf(ControlMode.Standby, ControlMode.PowerOff),
        selectedMode = state.selectedMode,
        onSelect = viewModel::selectMode
    )
}

@Composable
private fun ModeSection(
    title: String,
    modes: List<ControlMode>,
    selectedMode: ControlMode,
    onSelect: (ControlMode) -> Unit
) {
    Panel(padding = 12) {
        Text(title, color = RfText, fontSize = 14.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        modes.chunked(3).forEach { rowModes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowModes.forEach { mode ->
                    ModeButton(
                        mode = mode,
                        selected = selectedMode == mode,
                        onClick = { onSelect(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowModes.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ModeButton(
    mode: ControlMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (selected) RfBlue else RfPanelStrong
    val content = if (selected) Color.White else RfText
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        border = BorderStroke(1.dp, if (selected) RfBlue else RfLineSoft),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Text(mode.label, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun StrengthPage(state: ControlUiState, viewModel: ControlViewModel) {
    Panel {
        Text("强度调节", color = RfText, fontSize = 15.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        StrengthStepper("震动强度", state.levels.vibration, viewModel::updateVibration)
        StrengthStepper("拍打强度", state.levels.slap, viewModel::updateSlap)
        StrengthStepper("吮吸强度", state.levels.suction, viewModel::updateSuction)
        StrengthStepper("电击强度", state.levels.electric, viewModel::updateElectric)
        StrengthStepper("加热强度", state.levels.heat, viewModel::updateHeat)
    }

    Panel {
        Text("夹吸档位", color = RfText, fontSize = 15.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        ClipLevelSelector(value = state.levels.clip, onChange = viewModel::updateClip)
    }
}

@Composable
private fun StrengthStepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = RfText, fontSize = 13.sp, modifier = Modifier.weight(1f))
        StepButton("-", enabled = value > 0) { onChange(value - 1) }
        Text(
            "%02d".format(value.coerceIn(0, 100)),
            color = RfText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFFF5FA))
                .border(1.dp, RfLineSoft, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
        StepButton("+", enabled = value < 100) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(38.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = RfPanelStrong, contentColor = RfText),
        border = BorderStroke(1.dp, RfLineSoft),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ClipLevelSelector(value: Int, onChange: (Int) -> Unit) {
    val level = value.coerceIn(1, 6)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Button(
            onClick = { onChange(if (level >= 6) 1 else level + 1) },
            modifier = Modifier.size(132.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = RfBlue, contentColor = Color.White),
            border = BorderStroke(6.dp, Color(0xFFFFD54F)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(level.toString(), fontSize = 42.sp, fontWeight = FontWeight.Black)
                Text("档", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
@Composable
private fun DebugPage(state: ControlUiState) {
    Panel {
        MetricRow("设备码", state.deviceId)
        MetricRow("公司代码", state.companyIdText)
        MetricRow("预览 MAC", state.senderMac)
        MetricRow("目标 PDU", state.targetPduType)
        MetricRow("目标包头", state.targetPduHeader)
        MetricRow("AdvData 长度", "${state.txAdvData.size} 字节")
        MetricRow("发送计数", state.txCount.toString())
        MetricRow("广播状态", if (state.isAdvertising) "运行中" else "停止")
        MetricRow("说明", "真实 AdvA/CRC 由蓝牙控制器生成")
    }

    HexPanel("Name AD", state.txNameAdHex)
    HexPanel("Manufacturer AD", state.txManufacturerAdHex)
    HexPanel("协议 AdvData Byte9~Byte39", state.txAdvDataHex)
    HexPanel("完整 42 字节预览", state.txPacketHex)
    LogPanel(state.logs)
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = RfMuted, fontSize = 12.sp)
        Text(value, color = RfText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HexPanel(title: String, value: String) {
    Panel {
        Text(title, color = RfText, fontSize = 16.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        Text(
            value,
            color = RfTeal,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFFFF5FA))
                .border(1.dp, RfLineSoft, RoundedCornerShape(6.dp))
                .padding(10.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LogPanel(logs: List<EventLog>) {
    Panel {
        Text("最后发送内容", color = RfText, fontSize = 16.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        if (logs.isEmpty()) {
            Text("暂无发送内容", color = RfMuted)
        } else {
            logs.forEach { log ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFF5FA))
                        .border(1.dp, RfLineSoft, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        log.type.name.uppercase(),
                        color = logColor(log.type),
                        modifier = Modifier.weight(0.18f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(log.message, color = RfMuted, modifier = Modifier.weight(0.82f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, active: Boolean) {
    Surface(
        color = if (active) RfTeal.copy(alpha = 0.16f) else RfPanelStrong,
        contentColor = if (active) RfTeal else RfMuted,
        border = BorderStroke(1.dp, if (active) RfTeal.copy(alpha = 0.55f) else RfLine),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp)
    }
}

@Composable
private fun Panel(padding: Int = 16, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RfPanel)
            .border(1.dp, RfLine, RoundedCornerShape(8.dp))
            .padding(padding.dp)
    ) {
        content()
    }
}

private fun logColor(type: LogType): Color = when (type) {
    LogType.Ok, LogType.Rx -> RfTeal
    LogType.Tx -> RfGreen
    LogType.Warn -> RfAmber
    LogType.Error -> RfRed
    LogType.Info -> RfSubtle
}
