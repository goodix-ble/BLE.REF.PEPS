package com.goodix.ble.libcomx.util;

import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.event.IEventListener;

/**
 * Calculate data rate during measuring.
 * <p>
 * Call {@link #start()} and {@link #stop()} to control measurement.
 * <p>
 * Call {@link #addDataLength(int)} or subscribe a Event&lt;byte[]&gt; to accumulate data length and calculate data rate.
 * <p>
 * Subscribe {@link #evtUpdated()} and use getter to retrieve information.
 */
public class DataRateMeter implements Runnable, IEventListener<byte[]> {
    private static final String TAG = DataRateMeter.class.getSimpleName();

    public static final int EVT_UPDATED = 247;

    private Thread timerThread;
    private long updatePeriod = 1000;
    private int maxIdleCnt = 3;
    private final Event<Void> eventUpdated = new Event<>(this, EVT_UPDATED);

    private long totalByteCnt;
    private int tempByteCnt;

    private int speed;
    private int speedAvg;
    private boolean measuring = false;
    private long startTimestamp;
    private long stopTimestamp;

    public Event<Void> evtUpdated() {
        return eventUpdated;
    }

    public void setUpdatePeriod(long periodMillis) {
        if (periodMillis > 0) {
            this.updatePeriod = periodMillis;
        }
    }

    /**
     * Stop update speed if get N continuous zeros.
     */
    public void setMaxIdleCnt(int maxIdleCnt) {
        this.maxIdleCnt = maxIdleCnt;
    }

    /**
     * Start measuring average data rate.
     */
    public synchronized void start() {
        totalByteCnt = 0;
        tempByteCnt = 0;
        speed = 0;
        speedAvg = 0;
        stopTimestamp = startTimestamp = System.currentTimeMillis();
        measuring = true;

        // 由收到的数据来启动线程，瞬时速度从什么时候开始计算并不重要
        //if (timerThread == null) {
        //    timerThread = new Thread(this);
        //}
        //timerThread.start();
    }

    /**
     * Complete measuring average data rate.
     */
    public void stop() {
        boolean measuring;
        synchronized (this) {
            measuring = this.measuring;
            this.measuring = false;
            stopTimestamp = System.currentTimeMillis();
            speedAvg = (int) (totalByteCnt * 1000 / (stopTimestamp - startTimestamp));
        }
        // 通知最后更新的平均速率
        if (measuring) {
            eventUpdated.postEvent(null);
        }
        // 由线程自动停止
        //if (timerThread != null) {
        //    timerThread.interrupt();
        //}
        //timerThread = null;
    }

    /**
     * Retrieve average speed.
     */
    public int getSpeedAvg() {
        return speedAvg;
    }

    /**
     * Retrieve instant speed.
     */
    public int getSpeed() {
        return speed;
    }

    public long getElapsedTime() {
        if (measuring) {
            return System.currentTimeMillis() - startTimestamp;
        } else {
            return stopTimestamp - startTimestamp;
        }
    }

    public long getTotalDataLength() {
        return totalByteCnt;
    }

    public boolean isStarted() {
        return measuring;
    }

    public synchronized void addDataLength(int length) {
        if (measuring) {
            tempByteCnt += length;
            totalByteCnt += length;

            // 如果需要，就启动线程，以便统计即时数据
            // 这里判断为null时，线程可能正在退出，或者没有退出
            if (this.timerThread == null) {
                this.timerThread = new Thread(this, TAG);
                this.timerThread.start();
            }
        }
    }

    /**
     * Calculate data rate with Event.
     *
     * @deprecated Should be called by Event&lt;byte[]&gt;.
     */
    @Deprecated
    @Override
    public void onEvent(Object src, int evtType, byte[] evtData) {
        if (evtData != null && measuring) {
            addDataLength(evtData.length);
        }
    }

    @Override
    public void run() {
        Thread thread = this.timerThread;
        int idleCnt = 0;
        final int MAX_IDLE_CNT = maxIdleCnt;
        while (thread != null) {
            // 等待一个时间
            long start = System.currentTimeMillis();
            try {
                Thread.sleep(updatePeriod);
            } catch (InterruptedException e) {
                synchronized (this) {
                    this.timerThread = null;
                }
                break;
            }

            long stop = System.currentTimeMillis();
            long delta = stop - start;

            // 判断是否要继续计算
            if (!this.measuring) {
                synchronized (this) {
                    thread = null; // break loop
                    this.timerThread = null;
                }
                continue;
            }

            // 尽快对临时计数器清零
            int tempByteCnt;
            long totalByteCnt;
            boolean measuring;
            // Take a snapshot of counting variable
            synchronized (this) {
                tempByteCnt = this.tempByteCnt;
                // 清零
                this.tempByteCnt = 0;
                totalByteCnt = this.totalByteCnt;
                measuring = this.measuring;
                // 在同步块中更新速率，避免measuring与速率值不同步。
                // 因为调用stop()的时候，会同时更新measuring和speedAvg。
                if (measuring) {
                    speedAvg = (int) (totalByteCnt * 1000 / (stop - startTimestamp));
                }
            }

            if (measuring) {
                if (delta > 0) {
                    speed = (int) (tempByteCnt * 1000 / delta);
                } else {
                    speed = 0;
                }

                eventUpdated.postEvent(null);
            }

            // Stop this thread if there is no more data is received.
            if (MAX_IDLE_CNT > 0) {
                // 判断是否需要继续
                if (speed > 0) {
                    idleCnt = 0;
                } else {
                    idleCnt++;
                }

                // 连续收到N个0速度就停止线程
                if (idleCnt > MAX_IDLE_CNT) {
                    synchronized (this) {
                        thread = null; // break loop
                        this.timerThread = null;
                    }
                }
            }
        }
    }
}

