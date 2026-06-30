package com.example.rfcontrol.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ControlUiStateTest {
    @Test
    fun advertisingButtonLabelFollowsAdvertisingState() {
        assertEquals("开始冲浪", ControlUiState(isAdvertising = false).advertisingButtonLabel)
        assertEquals("停止冲浪", ControlUiState(isAdvertising = true).advertisingButtonLabel)
    }
}
