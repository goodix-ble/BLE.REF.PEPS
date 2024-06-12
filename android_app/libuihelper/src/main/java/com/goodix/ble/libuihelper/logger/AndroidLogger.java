package com.goodix.ble.libuihelper.logger;

import android.util.Log;

import com.goodix.ble.libcomx.ILogger;

public class AndroidLogger implements ILogger {
    private static final AndroidLogger INSTANCE = new AndroidLogger();

    public static AndroidLogger getInstance() {
        return INSTANCE;
    }

    @Override
    public void v(String tag, String msg) {
        Log.v(tag, msg);
    }

    @Override
    public void d(String tag, String msg) {
        Log.d(tag, msg);
    }

    @Override
    public void i(String tag, String msg) {
        Log.i(tag, msg);
    }

    @Override
    public void w(String tag, String msg) {
        Log.w(tag, msg);
    }

    @Override
    public void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    @Override
    public void e(String tag, String msg, Throwable e) {
        Log.e(tag, msg, e);
    }
}
