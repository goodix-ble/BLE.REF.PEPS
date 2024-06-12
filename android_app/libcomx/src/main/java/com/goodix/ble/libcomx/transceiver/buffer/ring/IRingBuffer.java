package com.goodix.ble.libcomx.transceiver.buffer.ring;

public interface IRingBuffer {
    /**
     * 获得缓存区里面总共有多少个有效字节
     */
    int getSize();

    /**
     * 获得缓存区还剩余多少字节可用
     */
    int getFreeSize();

    /**
     * 获取读指针的值，一般诊断的时候才会用到
     */
    int getReadPos();

    /**
     * 获取写指针的值，一般诊断的时候才会用到
     */
    int getWritePos();

    int get(int pos);

    int get(int pos, byte[] out);

    int getIntValue(int pos, int size, boolean bigEndian);
}
