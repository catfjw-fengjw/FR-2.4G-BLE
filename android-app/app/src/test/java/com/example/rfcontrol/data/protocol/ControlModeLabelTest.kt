package com.example.rfcontrol.data.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class ControlModeLabelTest {
    @Test
    fun modeLabelsMatchProductNamesWithoutChangingValues() {
        val expected = listOf(
            ControlMode.Mode1 to ("经典" to 0x31),
            ControlMode.Mode2 to ("太空" to 0x32),
            ControlMode.Mode3 to ("飞机" to 0x33),
            ControlMode.Mode4 to ("冲浪" to 0x34),
            ControlMode.Mode5 to ("强化" to 0x35),
            ControlMode.Mode6 to ("混沌" to 0x36),
            ControlMode.Mode7 to ("狂野" to 0x37),
            ControlMode.Mode8 to ("麻辣" to 0x38),
            ControlMode.Mode9 to ("黑化" to 0x39),
            ControlMode.Standby to ("休眠" to 0x3A),
            ControlMode.HeatOn to ("开启" to 0x3B),
            ControlMode.HeatOff to ("关闭" to 0x3C),
            ControlMode.PowerOff to ("停机" to 0x3D)
        )

        expected.forEach { (mode, expectedLabelAndValue) ->
            assertEquals(expectedLabelAndValue.first, mode.label)
            assertEquals(expectedLabelAndValue.second, mode.value)
        }
    }
}
