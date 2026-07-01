package com.example.rfcontrol.data.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RfPacketParserTest {
    @Test
    fun parsesLyDeviceStatusAndReadsBatteryFromByte32() {
        val packet = RfPacketBuilder.buildDevicePacket(
            device = RfDevice("111111", "B1:B2:B3:B4:B5:C1", -48, 90, "just now"),
            battery = 100
        )

        val status = RfPacketParser.parseDeviceStatus(packet, -48, expectedDeviceId = "111111")

        assertNotNull(status)
        status!!
        assertEquals("LY", status.namePrefix)
        assertEquals("111111", status.deviceId)
        assertEquals("B1:B2:B3:B4:B5:C1", status.mac)
        assertEquals(100, status.battery)
        assertEquals(-48, status.rssi)
        assertEquals(0x0000, status.companyId)
        assertEquals(ControlMode.Mode1, status.mode)
        assertEquals(0, status.levels.vibration)
        assertEquals(0, status.levels.heat)
    }

    @Test
    fun rejectsLxControlPacketsAsDeviceStatus() {
        val packet = RfPacketBuilder.buildControlPacket(
            deviceId = "111111",
            mode = ControlMode.Mode1,
            levels = StrengthLevels()
        )

        val status = RfPacketParser.parseDeviceStatus(packet, -45, expectedDeviceId = "111111")

        assertNull(status)
    }

    @Test
    fun rejectsPacketsForDifferentDeviceId() {
        val packet = RfPacketBuilder.buildDevicePacket(
            device = RfDevice("ABC123", "B1:B2:B3:B4:B5:C1", -48, 90, "just now"),
            battery = 88
        )

        val status = RfPacketParser.parseDeviceStatus(packet, -48, expectedDeviceId = "111111")

        assertNull(status)
    }
}
