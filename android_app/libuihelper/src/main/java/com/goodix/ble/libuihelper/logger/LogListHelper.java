package com.goodix.ble.libuihelper.logger;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SuppressWarnings({"FieldCanBeLocal", "WeakerAccess", "unused"})
public class LogListHelper implements Runnable {

    // 视图，可能在没有视图的情况下使用
    @Nullable
    private LogListHelper.LogAdapter logAdapter;
    @Nullable
    private RecyclerView logRecycleView;

    // 需要显示的数据
    private LinkedList<LogEntry> logEntries = new LinkedList<>();

    // 待处理的数据
    private ConcurrentLinkedQueue<LogEntry> logQueue = new ConcurrentLinkedQueue<>();
    private boolean hasPendingLog;
    private boolean hasPendingClear;
    private Handler uiHandler;
    private int updatePeriod = 500; // 处理的时间间隔
    private int defaultLevel = Log.DEBUG;

    // 自动保存数据到文件
    private boolean autoStoreToFile = false;
    private OutputStream logOutpuStream;
    private ConcurrentLinkedQueue<LogEntry> logQueueToStore;
    private Executor storeThread;
    private byte[] newLine;

    // 限制显示的日志的最大条数，避免长期运行的时候导致内存溢出
    private int autoClearThreshold = 0; // 当显示的日志条数大于等于该值时，就删除指定数量的日志
    private int autoClearRemoveCnt = 0;

    public LogListHelper attachView(RecyclerView logRecycleView) {
        logAdapter = new LogListHelper.LogAdapter();
        this.logRecycleView = logRecycleView;
        this.logRecycleView.setLayoutManager(new LinearLayoutManager(logRecycleView.getContext()));
        this.logRecycleView.setAdapter(logAdapter);
        return this;
    }

    public LogListHelper setHandler(Handler uiHandler) {
        this.uiHandler = uiHandler;
        return this;
    }

    public LogListHelper setUpdatePeriod(int periodMillis) {
        if (periodMillis < 1) {
            periodMillis = 1;
        }
        this.updatePeriod = periodMillis;
        return this;
    }

    public LogListHelper setAutoStoreToFile(OutputStream os) {
        if (os != null) {
            this.logOutpuStream = os;
            this.storeThread = Executors.newSingleThreadExecutor();
            this.logQueueToStore = new ConcurrentLinkedQueue<>();
            this.autoStoreToFile = true;
            newLine = "\r\n".getBytes();
        } else {
            this.autoStoreToFile = false;
        }
        return this;
    }

    public LogListHelper setAutoClear(int threshold, int removeCnt) {
        autoClearThreshold = threshold;
        autoClearRemoveCnt = removeCnt;
        return this;
    }

    public LogListHelper log(int color, CharSequence msg) {
        return log(defaultLevel, color, msg);
    }

    public LogListHelper log(int level, int color, CharSequence msg) {
        LogEntry entry = new LogEntry(System.currentTimeMillis(), level, color, msg);
        logQueue.add(entry);

        synchronized (this) {
            if (!hasPendingLog) {
                hasPendingLog = true;
                if (uiHandler == null) {
                    uiHandler = new Handler(Looper.getMainLooper());
                }
                uiHandler.postDelayed(this, updatePeriod);
            }
        }
        return this;
    }

    @Override
    public void run() {
        boolean toClear = hasPendingClear;

        synchronized (this) {
            hasPendingLog = false;
            hasPendingClear = false;
        }

        if (toClear) {
            logEntries.clear();
        }

        int startPos = logEntries.size();
        while (!logQueue.isEmpty()) {
            LogEntry entry = logQueue.poll();
            // 如果有视图，则添加到显示列表
            if (logRecycleView != null) {
                logEntries.add(entry);
            }
            // 如果要保存，则添加到写队列
            if (autoStoreToFile) {
                logQueueToStore.add(entry);
            }
        }
        int stopPos = logEntries.size();

        // 清除掉多余的日志再显示
        if (autoClearThreshold > 0 && autoClearRemoveCnt > 0) {
            // 循环清除到阈值一下
            while (logEntries.size() >= autoClearThreshold) {
                for (int i = 0; i < autoClearRemoveCnt; i++) {
                    logEntries.removeFirst();
                }
            }
        }

        if (logRecycleView != null && logAdapter != null) {
            if (stopPos > startPos) {
                //logAdapter.notifyItemRangeChanged(startPos, stopPos - startPos); 反而给人一种闪烁的感觉
                // 如果用户往上滑动了列表，就不要自动跳到最后了
                if (logRecycleView.canScrollVertically(1)) {
                    logAdapter.notifyDataSetChanged();
                } else {
                    logAdapter.notifyDataSetChanged();
                    //logRecycleView.smoothScrollToPosition(stopPos - 1); 会导致性能问题
                    logRecycleView.scrollToPosition(stopPos - 1);
                }
            } else {
                if (toClear) {
                    logAdapter.notifyDataSetChanged();
                }
            }
        }

        // 发起文件写操作
        if (autoStoreToFile) { // 防止NPE错误
            storeThread.execute(() -> {
                OutputStream tmpOs = logOutpuStream;
                if (tmpOs != null) {
                    try {
                        while (!logQueueToStore.isEmpty()) {
                            LogEntry entry = logQueueToStore.poll();
                            if (entry != null && entry.level != 100100) {
                                String msg = entry.timestamp + "," + entry.level + "," + entry.msg;
                                tmpOs.write(msg.getBytes());
                                tmpOs.write(newLine);
                            }
                        }
                        tmpOs.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                        // 停止写入到文件
                        autoStoreToFile = false;
                        // 写文件出错时，输出到屏幕上，并通过level的特殊值，避免再次触发写文件
                        log(100100, Color.RED, e.getMessage());
                        // 清除全部待写内容，退出循环
                        logQueueToStore.clear();
                    }
                }
            });
        }
    }


    public LogListHelper log(CharSequence msg) {
        return log(defaultLevel, Color.BLACK, msg);
    }

    public void setVisibility(int visibility) {
        if (this.logRecycleView != null) {
            this.logRecycleView.setVisibility(visibility);
        }
    }

    public void clearLog() {
        synchronized (this) {
            hasPendingClear = true;
            if (!hasPendingLog) {
                hasPendingLog = true;
                if (uiHandler == null) {
                    uiHandler = new Handler(Looper.getMainLooper());
                }
                uiHandler.postDelayed(this, 500);
            }
        }
    }

    private static class LogEntry {
        long timestamp;
        int level;
        @ColorInt
        int textColor;
        CharSequence msg;

        public LogEntry(long timestamp, int level, int textColor, CharSequence msg) {
            this.timestamp = timestamp;
            this.level = level;
            this.textColor = textColor;
            this.msg = msg;
        }
    }

    private class LogAdapter extends RecyclerView.Adapter<LogListHelper.LogAdapter.LogHolder> {
        private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
        private Date date = new Date();

        @NonNull
        @Override
        public LogListHelper.LogAdapter.LogHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            Context ctx = viewGroup.getContext();

            float density = ctx.getResources().getDisplayMetrics().density;
            int padding6dp = (int) (density * 6);
            int padding12dp = (int) (density * 12);


            LinearLayout root = new LinearLayout(ctx);
            root.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(0, 0, 0, padding6dp);

            TextView titleTv = new TextView(ctx);
            titleTv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            titleTv.setTextColor(Color.BLACK);

            TextView contentTv = new TextView(ctx);
            contentTv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            contentTv.setPaddingRelative(padding12dp, 0, 0, 0);

            root.addView(titleTv);
            root.addView(contentTv);

            return new LogListHelper.LogAdapter.LogHolder(root, titleTv, contentTv);
        }

        @Override
        public void onBindViewHolder(@NonNull LogListHelper.LogAdapter.LogHolder holder, int i) {
            LogListHelper.LogEntry entry = logEntries.get(i);
            date.setTime(entry.timestamp);
            holder.titleTv.setText(timeFormat.format(date));
            holder.contentTv.setText(entry.msg);
            holder.contentTv.setTextColor(entry.textColor);
        }

        @Override
        public int getItemCount() {
            return logEntries.size();
        }

        private class LogHolder extends RecyclerView.ViewHolder {

            TextView titleTv;
            TextView contentTv;

            LogHolder(@NonNull View itemView, TextView titleTv, TextView contentTv) {
                super(itemView);

                this.titleTv = titleTv;
                this.contentTv = contentTv;
            }
        }
    }
}
