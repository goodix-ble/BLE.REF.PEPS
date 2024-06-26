package com.goodix.ble.libuihelper.ble.scanner;

public class ScannedDeviceItem {
    public String name;
    public LeScannerReport report;

    public boolean isBonded;
    public boolean isConnected;

    private int rssiAvg;
    private int rssiSum;
    private int rssiCnt;

    public long lastUpdate;

    public void addRssi(int rssi) {
        this.rssiSum += rssi;
        this.rssiCnt++;
        this.rssiAvg = this.rssiSum / this.rssiCnt;
    }

    public int getRssiAvg() {
        return rssiAvg;
    }

    public void clearRssiCnt() {
        this.rssiSum = 0;
        this.rssiCnt = 0;
        this.rssiAvg = 0;
    }
}
