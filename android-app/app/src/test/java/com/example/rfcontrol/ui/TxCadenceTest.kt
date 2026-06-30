package com.example.rfcontrol.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TxCadenceTest {
    @Test
    fun steadyAdvertisingIntervalIs500Milliseconds() {
        assertEquals(500L, TxCadence.SteadyIntervalMillis)
    }

    @Test
    fun dataChangeBurstSendsThreePackets() {
        assertEquals(3, TxCadence.ChangeBurstPacketCount)
    }
}
