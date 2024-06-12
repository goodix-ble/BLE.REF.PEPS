package com.goodix.ble.libuihelper.thread;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

@SuppressWarnings("unused")
public class UiExecutor implements Executor {
    private static UiExecutor DEFAULT_EXECUTOR;

    private Handler mHandler;

    public static UiExecutor getDefault() {
        if (DEFAULT_EXECUTOR == null) {
            synchronized (UiExecutor.class) {
                if (DEFAULT_EXECUTOR == null) {
                    DEFAULT_EXECUTOR = new UiExecutor();
                }
            }
        }
        return DEFAULT_EXECUTOR;
    }

    public UiExecutor() {
        this(null);
    }

    public UiExecutor(Handler handler) {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        this.mHandler = handler;
    }

    public Handler getHandler() {
        return mHandler;
    }

    @Override
    public void execute(Runnable command) {
        mHandler.post(command);
    }
}
