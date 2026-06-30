package com.example.rfcontrol.data.protocol

object RfPacketParser {
    fun parseDeviceStatus(packet: ByteArray, rssi: Int): DeviceStatus? {
        if (packet.size != RfPacketBuilder.OverAirPacketSize) return null
        if ((packet[0].toInt() and 0xFF) != 0x42) return null
        if ((packet[1].toInt() and 0xFF) != 0x25) return null

        if ((packet[8].toInt() and 0xFF) != 0x09) return null
        if ((packet[9].toInt() and 0xFF) != 0x09) return null
        if (packet[10] != 0x4C.toByte() || packet[11] != 0x58.toByte()) return null

        val deviceId = packet.sliceArray(12..17).toString(Charsets.US_ASCII)
        val mac = packet.sliceArray(2..7).joinToString(":") {
            "%02X".format(it.toInt() and 0xFF)
        }
        return DeviceStatus(
            deviceId = deviceId,
            mac = mac,
            battery = packet[23].toInt() and 0xFF,
            rssi = rssi,
            packetHex = RfPacketBuilder.toHex(packet)
        )
    }
}
