package com.goodix.ble.libuihelper.logger;

import com.goodix.ble.libcomx.logger.RingLogger;

/**
 * Replace android.util.Log
 */
public class Log {
    private static RingLogger logger;

    static {
        logger = new RingLogger(20000);
        logger.setLogger(AndroidLogger.getInstance());
    }

    public static RingLogger getLogger() {
        return logger;
    }

    public static void v(String tag, String msg) {
        logger.v(tag, msg);
    }

    public static void d(String tag, String msg) {
        logger.d(tag, msg);
    }

    public static void i(String tag, String msg) {
        logger.i(tag, msg);
    }

    public static void w(String tag, String msg) {
        logger.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        logger.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable e) {
        logger.e(tag, msg, e);
    }
}
