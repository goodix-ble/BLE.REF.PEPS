package com.goodix.ble.libcomx.util;

import org.junit.Assert;
import org.junit.Test;

public class HexEndianTest {
    static byte[] rawLong = new byte[]{0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0};

    @Test
    public void fromByteLong() {
        Assert.assertEquals(0x123456789ABCDEF0L, HexEndian.fromByteLong(rawLong, 0, 8, true));
        Assert.assertEquals(0x123456789ABCDEL, HexEndian.fromByteLong(rawLong, 0, 7, true));
        Assert.assertEquals(0x123456789ABCL, HexEndian.fromByteLong(rawLong, 0, 6, true));
        Assert.assertEquals(0x123456789AL, HexEndian.fromByteLong(rawLong, 0, 5, true));
        Assert.assertEquals(0x12345678L, HexEndian.fromByteLong(rawLong, 0, 4, true));
        Assert.assertEquals(0x123456L, HexEndian.fromByteLong(rawLong, 0, 3, true));
        Assert.assertEquals(0x1234L, HexEndian.fromByteLong(rawLong, 0, 2, true));
        Assert.assertEquals(0x12L, HexEndian.fromByteLong(rawLong, 0, 1, true));

        Assert.assertEquals(0xF0DEBC9A78563412L, HexEndian.fromByteLong(rawLong, 0, 8, false));
        Assert.assertEquals(0xDEBC9A78563412L, HexEndian.fromByteLong(rawLong, 0, 7, false));
        Assert.assertEquals(0xBC9A78563412L, HexEndian.fromByteLong(rawLong, 0, 6, false));
        Assert.assertEquals(0x9A78563412L, HexEndian.fromByteLong(rawLong, 0, 5, false));
        Assert.assertEquals(0x78563412L, HexEndian.fromByteLong(rawLong, 0, 4, false));
        Assert.assertEquals(0x563412L, HexEndian.fromByteLong(rawLong, 0, 3, false));
        Assert.assertEquals(0x3412L, HexEndian.fromByteLong(rawLong, 0, 2, false));
        Assert.assertEquals(0x12L, HexEndian.fromByteLong(rawLong, 0, 1, false));
    }

    @Test
    public void toByteLong() {
        Assert.assertArrayEquals(new byte[]{0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0}, HexEndian.toByteLong(0x123456789ABCDEF0L, new byte[8], 0, 8, true));
        Assert.assertArrayEquals(new byte[]{0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0}, HexEndian.toByteLong(0x3456789ABCDEF0L, new byte[7], 0, 7, true));
        Assert.assertArrayEquals(new byte[]{0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0}, HexEndian.toByteLong(0x56789ABCDEF0L, new byte[6], 0, 6, true));
        Assert.assertArrayEquals(new byte[]{0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0}, HexEndian.toByteLong(0x789ABCDEF0L, new byte[5], 0, 5, true));
        Assert.assertArrayEquals(new byte[]{(byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0}, HexEndian.toByteLong(0x9ABCDEF0L, new byte[4], 0, 4, true));
        Assert.assertArrayEquals(new byte[]{(byte) 0xBC, (byte) 0xDE, (byte) 0xF0}, HexEndian.toByteLong(0xBCDEF0L, new byte[3], 0, 3, true));
        Assert.assertArrayEquals(new byte[]{(byte) 0xDE, (byte) 0xF0}, HexEndian.toByteLong(0xDEF0L, new byte[2], 0, 2, true));
        Assert.assertArrayEquals(new byte[]{(byte) 0xF0}, HexEndian.toByteLong(0xF0L, new byte[1], 0, 1, true));

        Assert.assertArrayEquals(new byte[]{0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0}, HexEndian.toByteLong(0xF0DEBC9A78563412L, new byte[8], 0, 8, false));
        Assert.assertArrayEquals(new byte[]{0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE}, HexEndian.toByteLong(0xF0DEBC9A78563412L, new byte[7], 0, 7, false));
        Assert.assertArrayEquals(new byte[]{0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC}, HexEndian.toByteLong(0xF0DEBC9A78563412L, new byte[6], 0, 6, false));
        Assert.assertArrayEquals(new byte[]{0x12, 0x34, 0x56, 0x78, (byte) 0x9A}, HexEndian.toByteLong(0xF0DEBC9A78563412L, new byte[5], 0, 5, false));
        Assert.assertArrayEquals(new byte[]{0x12, 0x34, 0x56, 0x78}, HexEndian.toByteLong(0xF0DEBC9A78563412L, new byte[4], 0, 4, false));
        Assert.assertArrayEquals(new byte[]{0x12, 0x34, 0x56}, HexEndian.toByteLong(0xF0DEBC9A78563412L, new byte[3], 0, 3, false));
        Assert.assertArrayEquals(new byte[]{0x12, 0x34}, HexEndian.toByteLong(0xF0DEBC9A78563412L, new byte[2], 0, 2, false));
        Assert.assertArrayEquals(new byte[]{0x12}, HexEndian.toByteLong(0xF0DEBC9A78563412L, new byte[1], 0, 1, false));
    }
}