package com.goodix.ble.libuihelper.dialog;

import android.app.Dialog;
import android.os.Handler;
import android.os.Looper;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AbsDelayedDialog implements Runnable {
    protected Dialog dialog;
    protected Handler timer = new Handler(Looper.getMainLooper());

    protected int delayForShow = 100; // 等待一段时间再显示
    protected int delayForDismiss = 500; // 必须显示一段时间才关闭
    protected boolean requestedShow = false;
    protected boolean requestedDismiss = false;

    public void setDelay(int beforeShowMillis, int beforeDismissMillis) {
        delayForShow = beforeShowMillis;
        delayForDismiss = beforeDismissMillis;
    }

    public void show() {
        // 还没有显示，也没有被请求显示过。
        if (dialog != null && !dialog.isShowing() && !requestedShow) {
            requestedShow = true;
            timer.removeCallbacks(this);
            timer.postDelayed(this, delayForShow);
            // Log.e("------", "show: request");
        }
    }

    public void dismiss() {
        if (dialog != null) {
            // 情况一：还处于显示delay状态
            if (requestedShow) {
                // 等待在回调里面清掉标志
                requestedDismiss = true;
                // Log.e("------", "dismiss: wait");
                return;
            }

            // 情况二：已经显示出来了,且还没有请求过关闭
            if (!requestedDismiss && dialog.isShowing()) {
                requestedDismiss = true;
                timer.removeCallbacks(this);
                timer.postDelayed(this, delayForDismiss);
                // Log.e("------", "dismiss: request");
            }
        }
    }

    @Override
    public void run() {
        // 优先处理关闭请求
        if (requestedDismiss) {
            requestedDismiss = false;
            requestedShow = false;
            // Log.e("------", "dismiss: done");
            if (dialog != null) {
                dialog.dismiss();
            }
        } else {
            if (requestedShow) {
                requestedShow = false;
                // Log.e("------", "show: done");
                if (dialog != null) {
                    dialog.show();
                }
            }
        }
    }
}
