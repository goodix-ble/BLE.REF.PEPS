package com.goodix.ble.libcomx;

public interface ILogger {
    void v(String tag, String msg);

    void d(String tag, String msg);

    void i(String tag, String msg);

    void w(String tag, String msg);

    void e(String tag, String msg);

    void e(String tag, String msg, Throwable e);

    /**
     * For cascading log.
     */
    default ILogger subLogger() {
        return this;
    }
}
