package com.example.rfcontrol.data.protocol

import java.util.Locale

object RfPacketBuilder {
    const val OverAirPacketSize = 42
    const val LegacyAdvDataSize = 31
    const val DefaultSenderMac = "11:11:11:11:11:11"
    private const val PrefixX = 0x58
    private const val PrefixY = 0x59
    private const val AdvDataStartIndex = 8
    private const val AdvDataEndIndex = 38

    fun isDeviceIdValid(deviceId: String): Boolean {
        return Regex("^[0-9A-Z]{6}$").matches(deviceId)
    }

    @JvmOverloads
    fun buildControlPacket(
        deviceId: String,
        mode: ControlMode,
        levels: StrengthLevels,
        senderMac: String = DefaultSenderMac,
        companyId: Int = 0x0000
    ): ByteArray {
        val bytes = ByteArray(OverAirPacketSize)
        val normalizedCompanyId = companyId.coerceIn(0x0000, 0xFFFF)
        bytes[0] = 0x42
        bytes[1] = 0x25
        parseMac(senderMac).forEachIndexed { index, value ->
            bytes[2 + index] = value.toByte()
        }
        bytes[8] = 0x09
        bytes[9] = 0x09
        bytes[10] = 0x4C
        bytes[11] = PrefixX.toByte()

        deviceIdToBytes(deviceId).forEachIndexed { index, value ->
            bytes[12 + index] = value.toByte()
        }

        bytes[18] = 0x14
        bytes[19] = 0xFF.toByte()
        bytes[20] = (normalizedCompanyId and 0xFF).toByte()
        bytes[21] = ((normalizedCompanyId shr 8) and 0xFF).toByte()
        bytes[22] = mode.value.toByte()
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
    @JvmOverloads
    fun buildControlAdvData(
        deviceId: String,
        mode: ControlMode,
        levels: StrengthLevels,
        senderMac: String = DefaultSenderMac,
        companyId: Int = 0x0000
    ): ByteArray {
        return buildControlPacket(deviceId, mode, levels, senderMac, companyId).sliceArray(AdvDataStartIndex..AdvDataEndIndex)
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
        bytes[11] = PrefixY.toByte()
        bytes[31] = battery.coerceIn(0, 100).toByte()
        return bytes
    }

    fun toHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(Locale.US, it.toInt() and 0xFF) }
    }

    fun byteHex(value: Int): String {
        return "0x%02X".format(Locale.US, value and 0xFF)
    }

    private fun deviceIdToBytes(deviceId: String): IntArray {
        val normalized = deviceId.uppercase(Locale.US).padEnd(6, '0').take(6)
        return normalized.map { it.code and 0xFF }.toIntArray()
    }

    private fun parseMac(mac: String): IntArray {
        val parts = mac.split(":")
        require(parts.size == 6) { "MAC must contain 6 bytes." }
        return parts.map { it.toInt(16) }.toIntArray()
    }
}
