package com.goodix.ble.libble.v2.gb.procedure;

public interface GBProcedureConnect extends GBProcedure {
    /**
     * 设置连接的重试参数
     *
     * @param maxCount       最大重试的次数
     * @param intervalMillis 重试前的等待时间，单位：毫秒
     */
    GBProcedureConnect setRetry(int maxCount, int intervalMillis);

    /**
     * 设置是否使用后台模式去创建连接。用后台模式会以节约能源的方式尝试一直保持与对端设备的连接。
     * 因为要节约能源，连接建立的速度可能会很慢。
     */
    GBProcedureConnect setBackgroundMode(boolean enable);
}
