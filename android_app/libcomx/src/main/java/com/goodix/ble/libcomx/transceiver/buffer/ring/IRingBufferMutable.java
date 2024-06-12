package com.goodix.ble.libcomx.transceiver.buffer.ring;

public interface IRingBufferMutable extends IRingBuffer {
    boolean put(byte dat);

    int put(byte[] dat, int pos, int size);

    int pop(int pos, byte[] out);

    void drop(int size);

    void dropAll();
}
