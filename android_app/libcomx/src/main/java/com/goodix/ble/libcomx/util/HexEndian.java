package com.goodix.ble.libcomx.util;

public class HexEndian {

    /**
     * if input is little endian, output is big endian
     * if input is big endian, output is little endian
     */
    public static int changeEndian(int org, int size) {
        int val = 0;
        for (int i = 0; i < size; i++) {
            val <<= 8;
            val |= org & 0xff;
            org >>= 8;
        }

        return val;
    }

    public static int fromByte(byte[] dat, int pos, int size, boolean bigEndian) {
        int val = 0;
        int end = pos + size;
        if (dat != null && pos >= 0 && size >= 0) {
            if (end > dat.length) {
                end = dat.length;
            }

            if (bigEndian) {
                for (int i = pos; i < end; i++) {
                    val <<= 8;
                    val |= (dat[i] & 0xFF);
                }
            } else {
                for (int i = end - 1; i >= pos; i--) {
                    val <<= 8;
                    val |= (dat[i] & 0xFF);
                }
            }
            return val;
        }
        return 0;
    }

    public static byte[] toByte(int val, byte[] out, int pos, int size, boolean bigEndian) {
        int end = pos + size;
        if (out != null && pos >= 0 && size >= 0) {
            if (end > out.length) {
                end = out.length;
            }

            if (bigEndian) {
                for (int i = end - 1; i >= pos; i--) {
                    out[i] = (byte) (val & 0xff);
                    val >>= 8;
                }
            } else {
                for (int i = pos; i < end; i++) {
                    out[i] = (byte) (val & 0xff);
                    val >>= 8;
                }
            }
        }
        return out;
    }

    /**
     * For long integer
     */
    public static long fromByteLong(byte[] dat, int pos, int size, boolean bigEndian) {
        long val = 0;
        int end = pos + size;
        if (dat != null && pos >= 0 && size >= 0) {
            if (end > dat.length) {
                end = dat.length;
            }

            if (bigEndian) {
                for (int i = pos; i < end; i++) {
                    val <<= 8;
                    val |= (dat[i] & 0xFF);
                }
            } else {
                for (int i = end - 1; i >= pos; i--) {
                    val <<= 8;
                    val |= (dat[i] & 0xFF);
                }
            }
            return val;
        }
        return 0;
    }

    public static byte[] toByteLong(long val, byte[] out, int pos, int size, boolean bigEndian) {
        int end = pos + size;
        if (out != null && pos >= 0 && size >= 0) {
            if (end > out.length) {
                end = out.length;
            }

            if (bigEndian) {
                for (int i = end - 1; i >= pos; i--) {
                    out[i] = (byte) (val & 0xff);
                    val >>= 8;
                }
            } else {
                for (int i = pos; i < end; i++) {
                    out[i] = (byte) (val & 0xff);
                    val >>= 8;
                }
            }
        }
        return out;
    }
}
