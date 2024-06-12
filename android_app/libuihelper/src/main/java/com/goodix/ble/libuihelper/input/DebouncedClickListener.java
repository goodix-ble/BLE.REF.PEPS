package com.goodix.ble.libuihelper.input;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

/**
 * 实现按钮的去抖动功能。也可以在去抖动的等待期间使控件进入禁止的状态。
 * 一个按键对应一个助手类。
 */
@SuppressWarnings("unused")
public class DebouncedClickListener implements View.OnClickListener, Runnable {
    private static final String TAG = "DebouncedClickListener";

    private long prvClickTimestamp = 0;

    /**
     * 消除抖动的间隔时间。在次时间内的点击事件将被忽略。
     * 单位：毫秒
     */
    private long interval = 500;

    private View.OnClickListener listener;

    private boolean antiBruteClick = false;

    /**
     * 实现按钮禁用状态配合去抖动状态
     */
    private boolean useDisableState = false;
    private View disabledView = null;
    private Handler timerHandler = null;

    public DebouncedClickListener(View.OnClickListener listener) {
        this.listener = listener;
    }

    public DebouncedClickListener(View.OnClickListener listener, long interval) {
        this.interval = interval;
        this.listener = listener;
    }

    public DebouncedClickListener setInterval(long interval) {
        this.interval = interval;
        return this;
    }

    /**
     * 启用防止暴力点击的功能。启用后，连续的点击只会触发1次点击事件。除非至少停止点击interval指定的毫秒数。
     */
    public DebouncedClickListener enableAntiBruteClick() {
        antiBruteClick = true;
        return this;
    }

    /**
     * 在去抖动的时间内，将控件设置为禁用状态。
     * 该功能会导致防止暴力点击功能不起作用。
     * <p>
     * 注意：该类会延时操作控件的禁用状态，请尽量不要再在其他地方设置该控件的禁用状态。并且注意资源释放问题。
     */
    public DebouncedClickListener enableUseDisableState() {
        useDisableState = true;
        return this;
    }

    public DebouncedClickListener enableUseDisableState(View targetView, Handler reuseHandlerAsTimer) {
        this.useDisableState = true;
        this.disabledView = targetView;
        this.timerHandler = reuseHandlerAsTimer;
        return this;
    }

    @Override
    public void onClick(View v) {

        long now = System.currentTimeMillis();

        if (now - prvClickTimestamp > interval) {
            prvClickTimestamp = now;
            listener.onClick(v);
        }

        if (antiBruteClick) {
            prvClickTimestamp = now;
        }

        if (useDisableState) {
            if (disabledView == null) {
                disabledView = v;
            }

            if (timerHandler == null) {
                timerHandler = new Handler(Looper.getMainLooper());
            }

            disabledView.setEnabled(false);
            timerHandler.postDelayed(this, interval);
        }
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        long remainTime = prvClickTimestamp + interval - now;

        if (remainTime > 0) {
            // 没有达到解锁条件就重新计时
            if (timerHandler != null) {
                timerHandler.postDelayed(this, remainTime);
            }
        } else {
            if (disabledView != null) {
                disabledView.setEnabled(true);
            }
        }
    }
}
