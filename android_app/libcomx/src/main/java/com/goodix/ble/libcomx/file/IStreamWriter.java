package com.goodix.ble.libcomx.file;

public interface IStreamWriter {
    boolean write(byte[] dat);

    void close();
}
