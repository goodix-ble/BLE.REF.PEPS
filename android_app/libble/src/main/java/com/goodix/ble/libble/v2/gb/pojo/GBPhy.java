package com.goodix.ble.libble.v2.gb.pojo;

public class GBPhy {
    public static final int LE_1M = 0x0000_0001;
    public static final int LE_2M = 0x0000_0002;
    public static final int LE_CODED = 0x0000_0004;
    public static final int LE_OPT_NO = 0x0000_0000;
    public static final int LE_OPT_S2 = 0x0001_0000;
    public static final int LE_OPT_S8 = 0x0002_0000;

    public int txPhy;
    public int rxPhy;
}
