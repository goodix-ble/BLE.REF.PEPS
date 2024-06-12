package com.goodix.ble.libble.v2.gb;

import com.goodix.ble.libcomx.ILogger;

/**
 * 实现通过广播发送数据的功能。
 * 按照Spec，这里发现的一个设备，指的是MAC地址和广播数据的集合
 */
public interface GBAdvertiser {
    void setLogger(ILogger logger);
}
