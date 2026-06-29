package com.example.rfcontrol.data.transport

import com.example.rfcontrol.data.protocol.ControlMode
import com.example.rfcontrol.data.protocol.RfPacketBuilder
import com.example.rfcontrol.data.protocol.StrengthLevels
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class BleDebugRfTransportTest {
    @Test
    fun servicePayloadStartsWithProtocolHeaderAndSenderMac() {
        val packet = RfPacketBuilder.buildControlPacket(
            deviceId = "111111",
            mode = ControlMode.Mode1,
            levels = StrengthLevels(42, 30, 56, 3, 0, 36),
            senderMac = "11:22:33:44:55:66"
        )

        val payload = BleDebugRfTransport.buildServicePayload(packet)

        assertEquals(24, payload.size)
        assertArrayEquals(
            byteArrayOf(0x42, 0x25, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66),
            payload.sliceArray(0..7)
        )
        assertArrayEquals(packet.sliceArray(13..28), payload.sliceArray(8..23))
    }
}
