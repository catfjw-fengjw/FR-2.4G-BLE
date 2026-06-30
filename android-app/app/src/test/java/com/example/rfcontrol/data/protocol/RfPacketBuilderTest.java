package com.example.rfcontrol.data.protocol;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RfPacketBuilderTest {
    @Test
    public void modesWriteExpectedByte23Values() {
        StrengthLevels levels = new StrengthLevels(42, 30, 56, 3, 0, 36);
        for (ControlMode mode : ControlMode.values()) {
            byte[] packet = RfPacketBuilder.INSTANCE.buildControlPacket("111111", mode, levels);
            assertEquals(mode.getValue(), packet[22] & 0xFF);
        }
    }

    @Test
    public void companyIdBytesRemainZero() {
        byte[] packet = RfPacketBuilder.INSTANCE.buildControlPacket(
                "111111",
                ControlMode.Mode1,
                new StrengthLevels(42, 30, 56, 3, 0, 36)
        );

        assertEquals(0x00, packet[20] & 0xFF);
        assertEquals(0x00, packet[21] & 0xFF);
    }

    @Test
    public void strengthFieldsMapToBytes24Through29() {
        StrengthLevels levels = new StrengthLevels(42, 30, 56, 3, 12, 36);
        byte[] packet = RfPacketBuilder.INSTANCE.buildControlPacket("111111", ControlMode.Mode1, levels);

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
                "111111",
                ControlMode.Mode1,
                new StrengthLevels(42, 30, 56, 3, 0, 36)
        );
        assertArrayEquals(new byte[]{0x55, 0x55, 0x55}, new byte[]{packet[39], packet[40], packet[41]});
    }

    @Test
    public void controlAdvDataIsByte9ThroughByte39() {
        StrengthLevels levels = new StrengthLevels(42, 30, 56, 3, 12, 36);
        byte[] packet = RfPacketBuilder.INSTANCE.buildControlPacket("111111", ControlMode.Mode1, levels);
        byte[] advData = RfPacketBuilder.INSTANCE.buildControlAdvData("111111", ControlMode.Mode1, levels);

        assertEquals(31, advData.length);
        for (int index = 0; index < advData.length; index++) {
            assertEquals(packet[index + 8], advData[index]);
        }
    }

    @Test
    public void defaultDeviceIdAdvDataMatchesV16Layout() {
        StrengthLevels levels = new StrengthLevels(42, 30, 56, 3, 0, 36);
        byte[] packet = RfPacketBuilder.INSTANCE.buildControlPacket("111111", ControlMode.Mode1, levels);
        byte[] advData = RfPacketBuilder.INSTANCE.buildControlAdvData("111111", ControlMode.Mode1, levels);

        assertEquals(42, packet.length);
        assertEquals(0x42, packet[0] & 0xFF);
        assertEquals(0x25, packet[1] & 0xFF);
        assertEquals(31, advData.length);
        assertArrayEquals(
                new byte[]{0x09, 0x09, 0x4C, 0x58, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31},
                new byte[]{
                        advData[0], advData[1], advData[2], advData[3], advData[4],
                        advData[5], advData[6], advData[7], advData[8], advData[9]
                }
        );
        assertArrayEquals(
                new byte[]{
                        0x14, (byte) 0xFF, 0x00, 0x00, 0x31,
                        0x2A, 0x1E, 0x38, 0x03, 0x00, 0x24,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00
                },
                new byte[]{
                        advData[10], advData[11], advData[12], advData[13], advData[14],
                        advData[15], advData[16], advData[17], advData[18], advData[19], advData[20],
                        advData[21], advData[22], advData[23], advData[24], advData[25], advData[26],
                        advData[27], advData[28], advData[29], advData[30]
                }
        );
    }

    @Test
    public void validatesExpectedDeviceIdFormat() {
        assertTrue(RfPacketBuilder.INSTANCE.isDeviceIdValid("111111"));
        assertTrue(RfPacketBuilder.INSTANCE.isDeviceIdValid("ABC123"));
        assertFalse(RfPacketBuilder.INSTANCE.isDeviceIdValid("ABC12345"));
        assertFalse(RfPacketBuilder.INSTANCE.isDeviceIdValid("LX_DX001"));
    }
}
