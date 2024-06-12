package com.goodix.ble.libcomx.pool;

public interface IRecyclable {
    void reuse(Pool pool);

    int getRefCnt();

    void retain();

    void release();
}
