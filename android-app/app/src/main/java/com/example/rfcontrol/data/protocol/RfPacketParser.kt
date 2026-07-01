package com.example.rfcontrol.data.protocol

import java.nio.charset.StandardCharsets
import java.util.Locale

object RfPacketParser {
    fun parseDeviceStatus(
        packet: ByteArray,
        rssi: Int,
        expectedDeviceId: String? = null
    ): DeviceStatus? {
        if (packet.size != RfPacketBuilder.OverAirPacketSize) return null
        if ((packet[0].toInt() and 0xFF) != 0x42) return null
        if ((packet[1].toInt() and 0xFF) != 0x25) return null
        if ((packet[8].toInt() and 0xFF) != 0x09) return null
        if ((packet[9].toInt() and 0xFF) != 0x09) return null
        if (packet[10] != 0x4C.toByte()) return null

        val namePrefix = String(packet.sliceArray(10..11), StandardCharsets.US_ASCII)
        if (namePrefix != "LY") return null

        val deviceId = String(packet.sliceArray(12..17), StandardCharsets.US_ASCII)
        if (expectedDeviceId != null && deviceId != expectedDeviceId.uppercase(Locale.US)) return null

        if ((packet[18].toInt() and 0xFF) != 0x14) return null
        if ((packet[19].toInt() and 0xFF) != 0xFF) return null

        val companyId = (packet[20].toInt() and 0xFF) or ((packet[21].toInt() and 0xFF) shl 8)
        val modeValue = packet[22].toInt() and 0xFF
        val mode = ControlMode.values().firstOrNull { it.value == modeValue } ?: return null
        val levels = StrengthLevels(
            vibration = packet[23].toInt() and 0xFF,
            slap = packet[24].toInt() and 0xFF,
            suction = packet[25].toInt() and 0xFF,
            clip = packet[26].toInt() and 0xFF,
            electric = packet[27].toInt() and 0xFF,
            heat = packet[28].toInt() and 0xFF
        )
        val mac = packet.sliceArray(2..7).joinToString(":") {
            "%02X".format(Locale.US, it.toInt() and 0xFF)
        }

        return DeviceStatus(
            namePrefix = namePrefix,
            deviceId = deviceId,
            companyId = companyId,
            mode = mode,
            levels = levels,
            mac = mac,
            battery = packet[31].toInt() and 0xFF,
            rssi = rssi,
            packetHex = RfPacketBuilder.toHex(packet)
        )
    }
}
