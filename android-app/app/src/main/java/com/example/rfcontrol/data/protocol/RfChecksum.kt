package com.example.rfcontrol.data.protocol

object RfChecksum {
    fun appChecksum(packet: ByteArray): Int {
        require(packet.size == RfPacketBuilder.OverAirPacketSize) {
            "RF over-air packet must be 42 bytes."
        }
        return packet.sliceArray(13..21).sumOf { it.toInt() and 0xFF } and 0xFF
    }

    fun deviceChecksum(macBytes: IntArray): Int {
        require(macBytes.size == 6) { "MAC must contain 6 bytes." }
        return macBytes.sumOf { it and 0xFF } and 0xFF
    }
}
