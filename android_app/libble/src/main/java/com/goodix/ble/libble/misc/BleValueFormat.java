package com.goodix.ble.libble.misc;

public class BleValueFormat {
    public static String getStringValue(final byte[] value, int offset) {
        if (value == null || offset > value.length) return null;
        byte[] strBytes = new byte[value.length - offset];
        for (int i = 0; i != (value.length - offset); ++i) strBytes[i] = value[offset + i];
        return new String(strBytes);
    }

    public static float getFloatValue(final byte[] value, int offset, int size) {
        if ((offset + size) > value.length) return 0;

        int mantissa = 0;
        int exponent = 0;
        switch (size) {
            case 2:
                mantissa = unsignedToSigned((value[offset] & 0xFF) + ((value[offset + 1] & 0x0F) << 8), 12);
                exponent = unsignedToSigned((value[offset + 1] & 0xFF) >> 4, 4);
                break;
            case 4:
                mantissa = unsignedToSigned((value[offset] & 0xFF) + ((value[offset + 1] & 0xFF) << 8) + ((value[offset + 2] & 0xFF) << 16), 24);
                exponent = value[offset + 3];
                break;
        }

        return (float) (mantissa * Math.pow(10, exponent));
    }

    /**
     * Convert an unsigned integer value to a two's-complement encoded
     * signed value.
     */
    private static int unsignedToSigned(int unsignedValue, int valueBitWidth) {
        int markOfSign = 1 << valueBitWidth - 1;
        if ((unsignedValue & markOfSign) != 0) {
            unsignedValue = -1 * (markOfSign - (unsignedValue & (markOfSign - 1)));
        }
        return unsignedValue;
    }
}
