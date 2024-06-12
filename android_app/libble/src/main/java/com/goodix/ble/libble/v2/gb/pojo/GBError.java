package com.goodix.ble.libble.v2.gb.pojo;

public class GBError extends Exception {
    private int errorCode;

    public GBError(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public GBError(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
