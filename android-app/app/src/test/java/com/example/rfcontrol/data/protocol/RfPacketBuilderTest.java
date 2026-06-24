package com.example.rfcontrol.data.protocol;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RfPacketBuilderTest {
    @Test
    public void modesWriteExpectedByte22Values() {
        StrengthLevels levels = new StrengthLevels(42, 30, 56, 3, 0, 36);
        for (ControlMode mode : ControlMode.values()) {
            byte[] packet = RfPacketBuilder.INSTANCE.buildControlPacket("LX_DX001", mode, levels);
            assertEquals(mode.getValue(), packet[21] & 0xFF);
        }
    }

    @Test
    public void appChecksumIsDeviceIdAndModeLowByte() {
        byte[] packet = RfPacketBuilder.INSTANCE.buildControlPacket(
                "LX_DX001",
                ControlMode.Mode1,
                new StrengthLevels(42, 30, 56, 3, 0, 36)
        );
        int expected = 0;
        for (int i = 13; i <= 21; i++) {
            expected += packet[i] & 0xFF;
        }
        expected &= 0xFF;

        assertEquals(expected, packet[22] & 0xFF);
        assertEquals(0x61, packet[22] & 0xFF);
    }

    @Test
    public void strengthFieldsMapToBytes24Through29() {
        StrengthLevels levels = new StrengthLevels(42, 30, 56, 3, 12, 36);
        byte[] packet = RfPacketBuilder.INSTANCE.buildControlPacket("LX_DX001", ControlMode.Mode1, levels);

        assertEquals(42, packet[23] & 0xFF);
        assertEquals(30, packet[24] & 0xFF);
        assertEquals(56, packet[25] & 0xFF);
        assertEquals(3, packet[26] & 0xFF);
        assertEquals(12, packet[27] & 0xFF);
        assertEquals(36, packet[28] & 0xFF);
    }

    @Test
    public void senderMacWritesToBytes3Through8() {
        byte[] packet = RfPacketBuilder.INSTANCE.buildControlPacket(
                "111111",
                ControlMode.Mode1,
                new StrengthLevels(42, 30, 56, 3, 0, 36),
                "11:22:33:44:55:66"
        );

        assertArrayEquals(
                new byte[]{0x11, 0x22, 0x33, 0x44, 0x55, 0x66},
                new byte[]{packet[2], packet[3], packet[4], packet[5], packet[6], packet[7]}
        );
    }

    @Test
    public void crcBytesRemainReserved55() {
        byte[] packet = RfPacketBuilder.INSTANCE.buildControlPacket(
                "LX_DX001",
                ControlMode.Mode1,
                new StrengthLevels(42, 30, 56, 3, 0, 36)
        );
        assertArrayEquals(new byte[]{0x55, 0x55, 0x55}, new byte[]{packet[39], packet[40], packet[41]});
    }

    @Test
    public void controlAdvDataIsByte9ThroughByte39() {
        StrengthLevels levels = new StrengthLevels(42, 30, 56, 3, 12, 36);
        byte[] packet = RfPacketBuilder.INSTANCE.buildControlPacket("LX_DX001", ControlMode.Mode1, levels);
        byte[] advData = RfPacketBuilder.INSTANCE.buildControlAdvData("LX_DX001", ControlMode.Mode1, levels);

        assertEquals(31, advData.length);
        for (int index = 0; index < advData.length; index++) {
            assertEquals(packet[index + 8], advData[index]);
        }
    }

    @Test
    public void validatesExpectedDeviceIdFormat() {
        assertTrue(RfPacketBuilder.INSTANCE.isDeviceIdValid("111111"));
        assertTrue(RfPacketBuilder.INSTANCE.isDeviceIdValid("ABC12345"));
        assertFalse(RfPacketBuilder.INSTANCE.isDeviceIdValid("LX_DX001"));
    }
}
