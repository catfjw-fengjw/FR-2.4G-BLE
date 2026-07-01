package com.example.rfcontrol.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ControlUiStateTest {
    @Test
    fun advertisingButtonLabelFollowsAdvertisingState() {
        assertEquals("开始冲浪", ControlUiState(isAdvertising = false).advertisingButtonLabel)
        assertEquals("停止冲浪", ControlUiState(isAdvertising = true).advertisingButtonLabel)
    }

    @Test
    fun connectionStateUsesTwentySecondFreshnessWindow() {
        val connected = ControlUiState(
            deviceId = "111111",
            lastRxDeviceId = "111111",
            lastRxBattery = 64,
            lastRxAtMillis = 1_000L,
            nowMillis = 20_999L
        )
        val disconnected = connected.copy(nowMillis = 21_001L)

        assertEquals(true, connected.isConnected)
        assertEquals(false, disconnected.isConnected)
        assertEquals("已连接", connected.connectionLabel)
        assertEquals("未连接", disconnected.connectionLabel)
    }

    @Test
    fun batteryDisplayKeepsLastValueAfterDisconnect() {
        val neverSeen = ControlUiState()
        val lastSeen = ControlUiState(
            deviceId = "111111",
            lastRxDeviceId = "111111",
            lastRxBattery = 87,
            lastRxAtMillis = 1_000L,
            nowMillis = 50_000L
        )

        assertEquals("--%", neverSeen.batteryText)
        assertEquals("87%", lastSeen.batteryText)
    }
}
