package com.goodix.ble.libcomx.transceiver;

public interface IFrameSender {
    boolean sendFrame(byte[] dat);
}
