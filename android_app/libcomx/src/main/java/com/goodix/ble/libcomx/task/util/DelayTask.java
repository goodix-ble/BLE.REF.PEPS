package com.goodix.ble.libcomx.task.util;

import com.goodix.ble.libcomx.task.Task;

/**
 * Caution: the timeout of parent task must be greater than delay/100.
 */
public class DelayTask extends Task {
    private int delay;
    private float timeElapsing; // for updating progress
    private float updateInterval = 0; // for updating progress.

    public DelayTask(int delayMillis) {
        this.delay = delayMillis;
    }

    public DelayTask setDelay(int delay) {
        this.delay = delay;
        return this;
    }

    @Override
    protected int doWork() {
        if (delay < 1) delay = 1;

        // limit to 10 days
        if (delay > 10 * 24 * 3600 * 1000) {
            finishedWithError("Delay is too long: " + delay);
        }

        startTimer(1, delay);

        // report progress every second or every percent
        timeElapsing = 0;
        updateInterval = delay / 100f;
        if (updateInterval < 1000f) {
            updateInterval = 1000f;
        }
        if (delay > updateInterval) {
            startTimer(2, (long) updateInterval, (long) updateInterval);
        }

        return delay + 1000;
    }

    @Override
    public void abort() {
        stopTimer();
        super.abort();
    }

    @Override
    protected void onTimeout(int id) {
        if (id == 1) {
            finishedWithDone();
        } else if (id == 2) {
            timeElapsing += updateInterval;
            publishProgress((int) (timeElapsing * 100f / delay));
        }
    }
}
