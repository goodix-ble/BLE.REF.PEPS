package com.goodix.ble.libuihelper.ble.scanner;

public interface ILeScannerFilter {
    boolean match(LeScannerReport report);
}
