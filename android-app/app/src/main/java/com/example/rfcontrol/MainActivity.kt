package com.example.rfcontrol

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfcontrol.data.protocol.EventLog
import com.example.rfcontrol.data.protocol.LogType
import com.example.rfcontrol.data.transport.BleDebugRfTransport
import com.example.rfcontrol.ui.ControlUiState
import com.example.rfcontrol.ui.ControlViewModel
import com.example.rfcontrol.ui.theme.RfAmber
import com.example.rfcontrol.ui.theme.RfBg
import com.example.rfcontrol.ui.theme.RfControlTheme
import com.example.rfcontrol.ui.theme.RfGreen
import com.example.rfcontrol.ui.theme.RfLine
import com.example.rfcontrol.ui.theme.RfLineSoft
import com.example.rfcontrol.ui.theme.RfMuted
import com.example.rfcontrol.ui.theme.RfPanel
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
        controlViewModel.setUseRealBle(true)
        requestBlePermissionsIfNeeded()
        setContent {
            RfControlTheme {
                val state by controlViewModel.uiState.collectAsState()
                SimpleControlConsole(state, controlViewModel)
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
private fun SimpleControlConsole(state: ControlUiState, viewModel: ControlViewModel) {
    Scaffold(containerColor = RfBg) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(RfBg)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("RF2.4G 控制台", color = RfText, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("BLE 广播发送", color = RfMuted, fontSize = 14.sp)
                }
                StatusPill(if (state.isAdvertising) "广播中" else "已停止", state.isAdvertising)
            }

            Panel {
                OutlinedTextField(
                    value = state.deviceId,
                    onValueChange = viewModel::updateDeviceId,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("设备 ID") },
                    supportingText = {
                        Text(
                            text = if (state.validDeviceId) "默认设备 ID：111111" else "请输入 1~8 位数字或大写字母",
                            color = if (state.validDeviceId) RfTeal else RfRed,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    singleLine = true
                )
                Spacer(Modifier.height(14.dp))
                Text("BLE 能力：${state.bleCapability}", color = RfMuted, fontSize = 13.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = viewModel::startAdvertising,
                    enabled = !state.isAdvertising && state.validDeviceId,
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RfTeal,
                        contentColor = Color(0xFF07110E)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("发送广播", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
                OutlinedButton(
                    onClick = viewModel::stopAdvertising,
                    enabled = state.isAdvertising,
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    border = BorderStroke(1.dp, if (state.isAdvertising) RfAmber else RfLine),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (state.isAdvertising) RfAmber else RfMuted
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("停止广播", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }

            Panel {
                MetricRow("设备 ID", state.deviceId)
                MetricRow("发送计数", state.txCount.toString())
                MetricRow("广播状态", if (state.isAdvertising) "运行中" else "停止")
            }

            LogPanel(state.logs.take(8))
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = RfMuted, fontSize = 14.sp)
        Text(value, color = RfText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LogPanel(logs: List<EventLog>) {
    Panel {
        Text("日志", color = RfText, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
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
                            .padding(10.dp)
                    ) {
                        Text(
                            log.type.name.uppercase(),
                            color = logColor(log.type),
                            modifier = Modifier.weight(0.18f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(log.message, color = RfMuted, modifier = Modifier.weight(0.82f), fontSize = 12.sp)
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
private fun Panel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RfPanel)
            .border(1.dp, RfLine, RoundedCornerShape(8.dp))
            .padding(16.dp)
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
