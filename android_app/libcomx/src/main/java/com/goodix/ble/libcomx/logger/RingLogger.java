package com.goodix.ble.libcomx.logger;

import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.annotation.Nullable;
import com.goodix.ble.libcomx.event.Event;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 基于内存的环形日志记录工具。
 * 日志采用异步的方式记录到数组中，并且可以同时输出到文件。
 * 当日志数量超过最大数组容量时，覆盖最早添加的日志。
 * 在日志更新后，可以等待一个间隔时间 {@link #setUpdateDelay(int)} 后再发出更新事件和写入文件。
 */

@SuppressWarnings({"unused", "WeakerAccess", "UnusedReturnValue"})
public class RingLogger implements Runnable, ILogger {

    public static final int EVT_UPDATE = 248;

    /**
     * Reference: android.util.Log
     */
    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;
    public static final int ASSERT = 7;

    private static final String TAG = RingLogger.class.getSimpleName();
    private static final int LVL_INNER_ERR = 100100;

    // 下一级Logger
    @Nullable
    private ILogger logger = null;

    // 记录日志的数组
    private LogEntry[] entries;
    private int writePos; // 记录下一个条日志写入的位置
    private int entryCnt; // 记录有多少条数据

    // 创建一个日志的拷贝，给外部的程序遍历
    @Nullable
    private ArrayList<LogEntry> entriesCopy = null;

    // 待处理的数据
    private boolean hasPendingLog;
    private boolean hasPendingClear;
    private int updateDelay = 200; // 等待一定的时间后才处理临时日志
    private String defaultTag = TAG;

    // IO 线程：处理通知和文件写入
    private Executor ioThread;

    @Nullable
    private Event<Void> eventUpdate = null;

    // 自动保存数据到文件
    private boolean autoStoreToFile = false;
    private ConcurrentLinkedQueue<LogEntry> storageQueue = null;
    private OutputStream logOutputStream;
    private byte[] newLine;

    public RingLogger(int maxCapability) {
        if (maxCapability < 1) maxCapability = 0;
        this.entries = new LogEntry[maxCapability];
        this.innerClear();
    }

    public Event<Void> evtUpdate() {
        if (eventUpdate == null) {
            synchronized (this) {
                if (eventUpdate == null) {
                    eventUpdate = new Event<>(this, EVT_UPDATE);
                }
            }
        }
        return eventUpdate;
    }

    public RingLogger setUpdateDelay(int millis) {
        if (millis < 1) {
            millis = 1;
        }
        this.updateDelay = millis;
        return this;
    }

    public RingLogger setLogger(ILogger logger) {
        this.logger = logger;
        return this;
    }

    public RingLogger setAutoStoreToFile(OutputStream os) {
        if (os != null) {
            this.logOutputStream = os;
            this.storageQueue = new ConcurrentLinkedQueue<>();
            this.autoStoreToFile = true;
            newLine = "\r\n".getBytes();
        } else {
            this.autoStoreToFile = false;
        }
        return this;
    }

    public RingLogger log(String msg) {
        return log(System.currentTimeMillis(), Thread.currentThread().getId(), DEBUG, defaultTag, msg);
    }

    public RingLogger log(int level, String tag, String msg) {
        return log(System.currentTimeMillis(), Thread.currentThread().getId(), level, tag, msg);
    }

    public RingLogger log(long timestamp, long tid, int level, String tag, String msg) {
        final int max = entries.length;
        LogEntry existEntry;

        synchronized (this) {
            // 尝试利用以及存在的记录
            existEntry = entries[writePos];
            if (existEntry != null) {
                existEntry.timestamp = timestamp;
                existEntry.tid = tid;
                existEntry.level = level;
                existEntry.tag = tag;
                existEntry.msg = msg;
            } else {
                entries[writePos] = existEntry = new LogEntry(timestamp, tid, level, tag, msg);
            }

            writePos++;
            if (writePos >= max) {
                writePos -= max;
            }
            if (entryCnt < max) {
                entryCnt++;
            }
        }

        // 需要保存文件时才为每条记录创建一个对象

        if (autoStoreToFile) {
            storageQueue.add(new LogEntry(existEntry));
        }

        // 请求在异步线程进行处理和通知
        requestNotify();
        return this;
    }

    public void clear() {
        synchronized (this) {
            hasPendingClear = true;
        }
        requestNotify();
    }

    public void clearSync() {
        innerClear();
        // 分发事件
        Event<Void> event = this.eventUpdate;
        if (event != null) {
            event.postEvent(null);
        }
    }

    private synchronized void requestNotify() {
        if (!hasPendingLog) {
            hasPendingLog = true;
            if (ioThread == null) {
                ioThread = new ThreadPoolExecutor(0, 1,
                        5000L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>());
            }
            ioThread.execute(this);
        }
    }

    public int size() {
        return entryCnt;
    }

    /**
     * 非线程安全，请谨慎使用
     */
    @Deprecated
    public LogEntry get(int i) {
        if (entryCnt < entries.length) {
            return entries[i];
        } else {
            int readPos = (writePos + i) % entries.length;
            return entries[readPos];
        }
    }

    /**
     * 当 out 为 null 时，非线程安全，在单线程上使用没有问题，会使用内部一个共享的列表
     * 当 out 不为空时，线程安全，结果会存储到out中。如果 out 不为空列表，该函数会重用 out 中的元素
     */
    public List<LogEntry> getLogs(List<LogEntry> out) {
        int existItemCntInOut;
        int itemCntInEntries;

        synchronized (this) {
            if (out == null) {
                if (entriesCopy == null) {
                    entriesCopy = new ArrayList<>(entries.length);
                }
                out = entriesCopy;
            }

            existItemCntInOut = out.size();
            itemCntInEntries = entryCnt;

            for (int i = 0; i < entryCnt; i++) {
                LogEntry entry;

                if (entryCnt < entries.length) {
                    // 需要拷贝一下，否则entry内容可能被篡改
                    entry = entries[i];
                } else {
                    int readPos = (writePos + i) % entries.length;
                    entry = entries[readPos];
                }

                // 尽量重用 out 中的元素，减少实例化次数
                if (i < existItemCntInOut) {
                    out.get(i).copy(entry);
                } else {
                    out.add(new LogEntry(entry));
                }
            }
        }

        // 移除多余的项
        while (itemCntInEntries < existItemCntInOut) {
            existItemCntInOut--;
            out.remove(existItemCntInOut);
        }

        return out;
    }

    public int getCapability() {
        return entries.length;
    }

    public void saveTo(File outputFile) throws IOException {
        if (outputFile == null) {
            return;
        }
        if (!outputFile.exists()) {
            final File parentFile = outputFile.getParentFile();
            if (!parentFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parentFile.mkdirs();
            }
            //noinspection ResultOfMethodCallIgnored
            outputFile.createNewFile();
        }
        if (outputFile.exists()) {
            try (FileOutputStream os = new FileOutputStream(outputFile)) {
                saveTo(os);
            }
        }
    }

    public void saveTo(OutputStream tmpOs) throws IOException {
        final List<LogEntry> logs = getLogs(new ArrayList<>(getCapability()));
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        StringBuilder sb = new StringBuilder(1024);

        if (newLine == null) {
            newLine = "\r\n".getBytes();
        }

        for (LogEntry entry : logs) {
            if (entry != null) {
                sb.delete(0, sb.length());
                // time
                date.setTime(entry.timestamp);
                sb.append(dateFormat.format(date));
                // tid
                sb.append(" ");
                sb.append(entry.tid);
                // level,tag,message
                sb.append(" ");
                sb.append(entry.level);
                sb.append(" ");
                sb.append(entry.tag);
                sb.append(": ");
                sb.append(entry.msg);

                tmpOs.write(sb.toString().getBytes());
                tmpOs.write(newLine);
            }
        }
        tmpOs.flush();
    }

    /**
     * 在IO线程中，保存日志到文件中，并且通知外部日志数据已变化
     */
    @Override
    public void run() {

        int delay = updateDelay;
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ignored) {
            }
        }

        boolean toClear = hasPendingClear;

        synchronized (this) {
            hasPendingLog = false;
            hasPendingClear = false;
        }

        if (toClear) {
            innerClear();
        }

        OutputStream tmpOs = null;

        if (autoStoreToFile) {
            tmpOs = logOutputStream;
        }

        // 如果要保存，就写入到文件
        if (tmpOs != null) {
            try {
                Date date = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
                StringBuilder sb = new StringBuilder(1024);
                while (!storageQueue.isEmpty()) {
                    LogEntry entry = storageQueue.poll();
                    if (entry != null && entry.level != LVL_INNER_ERR) {
                        sb.delete(0, sb.length());
                        // time
                        date.setTime(entry.timestamp);
                        sb.append(dateFormat.format(date));
                        // tid
                        sb.append(" ");
                        sb.append(entry.tid);
                        // level,tag,message
                        sb.append(" ");
                        sb.append(entry.level);
                        sb.append(" ");
                        sb.append(entry.tag);
                        sb.append(": ");
                        sb.append(entry.msg);

                        tmpOs.write(sb.toString().getBytes());
                        tmpOs.write(newLine);
                    }
                }
                tmpOs.flush();
            } catch (Exception e) {
                e.printStackTrace();
                // 停止写入到文件
                autoStoreToFile = false;
                // 写文件出错时，输出到屏幕上，并通过level的特殊值，避免再次触发写文件
                log(LVL_INNER_ERR, TAG, e.getMessage());
            }
        }

        // 分发事件
        Event<Void> event = this.eventUpdate;
        if (event != null) {
            event.postEvent(null);
        }
    }

    private synchronized void innerClear() {
        this.entryCnt = 0;
        this.writePos = 0;
    }

    public static class LogEntry {
        public long timestamp;
        public long tid; // Thread ID
        public int level;
        public String tag;
        public String msg;

        LogEntry(long timestamp, long tid, int level, String tag, String msg) {
            this.timestamp = timestamp;
            this.tid = tid;
            this.level = level;
            this.tag = tag;
            this.msg = msg;
        }

        LogEntry(LogEntry other) {
            this.copy(other);
        }

        public void copy(LogEntry entry) {
            this.timestamp = entry.timestamp;
            this.tid = entry.tid;
            this.level = entry.level;
            this.tag = entry.tag;
            this.msg = entry.msg;
        }
    }

    @Override
    public void v(String tag, String msg) {
        log(System.currentTimeMillis(), Thread.currentThread().getId(), VERBOSE, tag, msg);
        if (logger != null) {
            logger.v(tag, msg);
        }
    }

    @Override
    public void d(String tag, String msg) {
        log(System.currentTimeMillis(), Thread.currentThread().getId(), DEBUG, tag, msg);
        if (logger != null) {
            logger.d(tag, msg);
        }
    }

    @Override
    public void i(String tag, String msg) {
        log(System.currentTimeMillis(), Thread.currentThread().getId(), INFO, tag, msg);
        if (logger != null) {
            logger.i(tag, msg);
        }
    }

    @Override
    public void w(String tag, String msg) {
        log(System.currentTimeMillis(), Thread.currentThread().getId(), WARN, tag, msg);
        if (logger != null) {
            logger.w(tag, msg);
        }
    }

    @Override
    public void e(String tag, String msg) {
        log(System.currentTimeMillis(), Thread.currentThread().getId(), ERROR, tag, msg);
        if (logger != null) {
            logger.e(tag, msg);
        }
    }

    @Override
    public void e(String tag, String msg, Throwable e) {
        log(System.currentTimeMillis(), Thread.currentThread().getId(), ERROR, tag, msg);
        if (logger != null) {
            logger.e(tag, msg, e);
        }
    }
}
