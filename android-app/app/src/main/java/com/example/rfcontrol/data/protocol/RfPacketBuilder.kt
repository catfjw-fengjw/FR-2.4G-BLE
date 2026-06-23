package com.example.rfcontrol.data.protocol

import java.util.Locale

object RfPacketBuilder {
    const val OverAirPacketSize = 42
    const val LegacyAdvDataSize = 31
    private const val AdvDataStartIndex = 8
    private const val AdvDataEndIndex = 38

    fun isDeviceIdValid(deviceId: String): Boolean {
        return Regex("^LX_DX[0-9A-Z]{3}$").matches(deviceId)
    }

    fun buildControlPacket(
        deviceId: String,
        mode: ControlMode,
        levels: StrengthLevels
    ): ByteArray {
        val bytes = ByteArray(OverAirPacketSize)
        bytes[0] = 0x42
        bytes[1] = 0x25
        bytes[8] = 0x02
        bytes[9] = 0x01
        bytes[10] = 0x06
        bytes[11] = 0x0B
        bytes[12] = 0x09

        deviceIdToBytes(deviceId).forEachIndexed { index, value ->
            bytes[13 + index] = value.toByte()
        }

        bytes[21] = mode.value.toByte()
        bytes[22] = RfChecksum.appChecksum(bytes).toByte()
        bytes[23] = levels.vibration.coerceIn(0, 100).toByte()
        bytes[24] = levels.slap.coerceIn(0, 100).toByte()
        bytes[25] = levels.suction.coerceIn(0, 100).toByte()
        bytes[26] = levels.clip.coerceIn(0, 6).toByte()
        bytes[27] = levels.electric.coerceIn(0, 100).toByte()
        bytes[28] = levels.heat.coerceIn(0, 100).toByte()
        bytes[39] = 0x55
        bytes[40] = 0x55
        bytes[41] = 0x55
        return bytes
    }

    /**
     * Returns Byte9~Byte39, i.e. the 31-byte BLE legacy AdvData portion.
     *
     * The complete on-air RF packet is still 42 bytes:
     * 2-byte BLE advertising PDU header + 6-byte advertiser address + 31-byte
     * AdvData + 3-byte link-layer CRC.
     *
     * Android application code should hand only this 31-byte AdvData concept to
     * the BLE advertising layer. Header, advertiser address, and CRC are handled
     * by the controller/link layer.
     */
    fun buildControlAdvData(
        deviceId: String,
        mode: ControlMode,
        levels: StrengthLevels
    ): ByteArray {
        return buildControlPacket(deviceId, mode, levels).sliceArray(AdvDataStartIndex..AdvDataEndIndex)
    }

    fun buildDevicePacket(device: RfDevice, battery: Int): ByteArray {
        val bytes = buildControlPacket(
            deviceId = device.id,
            mode = ControlMode.Mode1,
            levels = StrengthLevels(0, 0, 0, 0, 0, 0)
        )
        val macBytes = parseMac(device.mac)
        macBytes.forEachIndexed { index, value ->
            bytes[2 + index] = value.toByte()
        }
        bytes[21] = 0x00
        bytes[22] = RfChecksum.deviceChecksum(macBytes).toByte()
        bytes[23] = battery.coerceIn(0, 100).toByte()
        return bytes
    }

    fun toHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(Locale.US, it.toInt() and 0xFF) }
    }

    fun byteHex(value: Int): String {
        return "0x%02X".format(Locale.US, value and 0xFF)
    }

    private fun deviceIdToBytes(deviceId: String): IntArray {
        val normalized = deviceId.padEnd(8, '0').take(8)
        return normalized.map { it.code and 0xFF }.toIntArray()
    }

    private fun parseMac(mac: String): IntArray {
        val parts = mac.split(":")
        require(parts.size == 6) { "MAC must contain 6 bytes." }
        return parts.map { it.toInt(16) }.toIntArray()
    }
}
