package com.goodix.ble.libble.v2.impl.data;

public class BleCI {
    public int interval;
    public int latency;
    public int timeout;

    public BleCI(int interval, int latency, int timeout) {
        this.interval = interval;
        this.latency = latency;
        this.timeout = timeout;
    }
}
