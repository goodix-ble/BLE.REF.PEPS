package com.goodix.ble.libuihelper.input;

import android.view.View;

/**
 * This class is most likely to be used when we need to hide some hack functions.
 * It truly triggers the click event when clicking the view N times in time.
 */
@SuppressWarnings({"unused", "WeakerAccess", "UnusedReturnValue"})
public class CountedClickListener implements View.OnClickListener {
    private View.OnClickListener listener;
    private long tapStartTime = 0;
    private int tapCount = 0;
    private int triggerCount = 10;
    private int timeLimit = 5000;

    public CountedClickListener() {
    }

    public CountedClickListener(int timeoutMillis, int count, View.OnClickListener listener) {
        setTrigger(timeoutMillis, count);
        this.listener = listener;
    }

    public CountedClickListener setTrigger(int timeoutMillis, int count) {
        triggerCount = count;
        timeLimit = timeoutMillis;
        if (triggerCount < 1) {
            triggerCount = 1;
        }
        if (timeLimit < 100) {
            timeLimit = 100;
        }
        return this;
    }

    public CountedClickListener setListener(View.OnClickListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public void onClick(View v) {
        long now = System.currentTimeMillis();

        if (now - tapStartTime > timeLimit) {
            tapCount = 0;
            tapStartTime = now;
        }

        tapCount++;
        if (tapCount < triggerCount) {
            return;
        }

        if (listener != null) {
            tapCount = 0;
            tapStartTime = 0;
            listener.onClick(v);
        }
    }
}
