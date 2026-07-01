package com.example.rfcontrol.data.transport

import com.example.rfcontrol.data.protocol.ControlMode
import com.example.rfcontrol.data.protocol.RfPacketBuilder
import com.example.rfcontrol.data.protocol.StrengthLevels
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class BleDebugRfTransportTest {
    @Test
    fun protocolAdvDataIsByte9ThroughByte39() {
        val packet = RfPacketBuilder.buildControlPacket(
            deviceId = "111111",
            mode = ControlMode.Mode1,
            levels = StrengthLevels(42, 30, 56, 3, 0, 36),
            senderMac = "11:22:33:44:55:66"
        )

        val advData = BleDebugRfTransport.buildProtocolAdvData(packet)

        assertEquals(31, advData.size)
        assertArrayEquals(
            byteArrayOf(0x09, 0x09, 0x4C, 0x58, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31),
            advData.sliceArray(0..9)
        )
        assertArrayEquals(packet.sliceArray(8..38), advData)
    }

    @Test
    fun localNameCarrierUsesV16NameAndManufacturerData() {
        val packet = RfPacketBuilder.buildControlPacket(
            deviceId = "111111",
            mode = ControlMode.Mode1,
            levels = StrengthLevels(42, 30, 56, 3, 0, 36)
        )

        val advData = BleDebugRfTransport.buildProtocolAdvData(packet)
        val localName = BleDebugRfTransport.buildProtocolLocalName(advData)
        val manufacturerData = BleDebugRfTransport.buildManufacturerPayload(advData)

        assertEquals(8, localName.length)
        assertEquals("LX111111", localName)
        assertEquals(17, manufacturerData.size)
        assertArrayEquals(
            byteArrayOf(0x31, 0x2A, 0x1E, 0x38, 0x03, 0x00, 0x24, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
            manufacturerData
        )
        assertArrayEquals(
            byteArrayOf(
                0x14, 0xFF.toByte(), 0x00, 0x00,
                0x31, 0x2A, 0x1E, 0x38, 0x03, 0x00, 0x24,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
            ),
            BleDebugRfTransport.buildManufacturerAdStructure(advData)
        )
    }

    @Test
    fun manufacturerCompanyIdComesFromProtocolAdvData() {
        val packet = RfPacketBuilder.buildControlPacket(
            deviceId = "ABC123",
            mode = ControlMode.Mode1,
            levels = StrengthLevels(42, 30, 56, 3, 0, 36),
            companyId = 0x1234
        )

        val advData = BleDebugRfTransport.buildProtocolAdvData(packet)

        assertEquals(0x1234, BleDebugRfTransport.buildManufacturerCompanyId(advData))
        assertArrayEquals(
            byteArrayOf(
                0x14, 0xFF.toByte(), 0x34, 0x12,
                0x31, 0x2A, 0x1E, 0x38, 0x03, 0x00, 0x24,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
            ),
            BleDebugRfTransport.buildManufacturerAdStructure(advData)
        )
    }

    @Test
    fun scanPayloadCanBeRebuiltIntoV17DevicePreviewPacket() {
        val payload = byteArrayOf(
            0x31,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00,
            0x64,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        )

        val packet = BleDebugRfTransport.buildDeviceStatusPreviewPacket(
            localName = "LY111111",
            companyId = 0x0000,
            manufacturerPayload = payload,
            advertiserMac = "B1:B2:B3:B4:B5:C1"
        )

        assertArrayEquals(
            byteArrayOf(0x42, 0x25, 0xB1.toByte(), 0xB2.toByte(), 0xB3.toByte(), 0xB4.toByte(), 0xB5.toByte(), 0xC1.toByte()),
            packet.sliceArray(0..7)
        )
        assertArrayEquals(
            byteArrayOf(0x09, 0x09, 0x4C, 0x59, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31),
            packet.sliceArray(8..17)
        )
        assertEquals(0x64, packet[31].toInt() and 0xFF)
        assertArrayEquals(byteArrayOf(0x55, 0x55, 0x55), packet.sliceArray(39..41))
    }
}
