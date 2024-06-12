package com.goodix.ble.libcomx.util;

public class IntUtil {
    public static int compareUnsignedInt(int v1, int v2) {
        long val1 = 0xFFFF_FFFFL & v1;
        long val2 = 0xFFFF_FFFFL & v2;
        long delta = val1 - val2;
        if (delta > 0) {
            return 1;
        } else if (delta < 0) {
            return -1;
        }
        return 0;
    }

    public static boolean valueInRange(int min, int max, int val) {
        return val >= min && val <= max;
    }

    public static boolean rangeOverlap(int min1, int max1, int min2, int max2) {
        return valueInRange(min1, max1, min2) || valueInRange(min1, max1, max2);
    }

    public static boolean valueInRangeUnsignedInt(int min, int max, int val) {
        long minL = 0xFFFF_FFFFL & min;
        long maxL = 0xFFFF_FFFFL & max;
        long valL = 0xFFFF_FFFFL & val;
        return valL >= minL && valL <= maxL;
    }

    public static boolean rangeOverlapUnsignedInt(int min1, int max1, int min2, int max2) {
        return valueInRangeUnsignedInt(min1, max1, min2) || valueInRangeUnsignedInt(min1, max1, max2);
    }

    public static boolean memoryOverlap(int srcStart, int srcSize, int dstStart, int dstSize) {
        long dstStartL = 0xFFFF_FFFFL & dstStart;
        long dstEndL = dstStartL + (0xFFFF_FFFFL & dstSize);
        long srcStartL = 0xFFFF_FFFFL & srcStart;
        long srcEndL = srcStartL + (0xFFFF_FFFFL & srcSize);
        //return !(srcEndL <= dstStartL || srcStartL >= dstEndL);
        return srcEndL > dstStartL && srcStartL < dstEndL;
    }
}
