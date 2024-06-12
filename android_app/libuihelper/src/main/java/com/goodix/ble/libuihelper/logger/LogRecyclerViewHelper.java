package com.goodix.ble.libuihelper.logger;

import android.content.Context;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libcomx.logger.Logger;
import com.goodix.ble.libcomx.logger.RingLogger;
import com.goodix.ble.libcomx.util.HexStringBuilder;
import com.goodix.ble.libuihelper.input.DebouncedClickListener;
import com.goodix.ble.libuihelper.thread.UiExecutor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

@SuppressWarnings({"unused", "WeakerAccess", "UnusedReturnValue", "FieldCanBeLocal"})
public class LogRecyclerViewHelper extends RecyclerView.OnScrollListener implements IEventListener
        , TextView.OnEditorActionListener
        , View.OnClickListener
        , AdapterView.OnItemSelectedListener {
    private static final String TAG = LogRecyclerViewHelper.class.getSimpleName();

    // 视图，可能在没有视图的情况下使用
    @Nullable
    private LogRecyclerViewHelper.LogAdapter logAdapter;
    @Nullable
    private RecyclerView logRecycleView;
    @Nullable
    private View clearBtn, saveBtn, autoScrollBtn;
    @Nullable
    private Spinner logLevelSpinner;
    @Nullable
    private EditText searchEd;

    @Nullable
    private RingLogger ringLogger = null;
    private SparseIntArray colorMap = new SparseIntArray();

    private int logLevel = Log.VERBOSE;
    @Nullable
    private String logFilter;
    private final ArrayList<RingLogger.LogEntry> cleanLogList = new ArrayList<>();
    private final ArrayList<RingLogger.LogEntry> logCopyList = new ArrayList<>();
    private boolean hideTag = false;

    private boolean scrollToBottom = false;

    private boolean pauseUpdate = false; // 暂停更新，用于View不可见的时候节省计算
    private boolean hasUpdateWhilePause = false; // 暂停更新时，是否有更新，在解除暂停的时候，判断是否要刷新UI

    public LogRecyclerViewHelper() {
        colorMap.put(Log.VERBOSE, Color.GRAY);
        colorMap.put(Log.DEBUG, Color.BLACK);
        colorMap.put(Log.INFO, Color.BLUE);
        colorMap.put(Log.WARN, Color.MAGENTA);
        colorMap.put(Log.ERROR, Color.RED);
        colorMap.put(Log.ASSERT, Color.RED);
    }

    public LogRecyclerViewHelper(RingLogger ringLogger) {
        this();
        setRingLogger(ringLogger);
    }

    @Nullable
    public RingLogger getRingLogger() {
        return ringLogger;
    }

    public LogRecyclerViewHelper setRingLogger(RingLogger ringLogger) {
        RingLogger prvLogger = this.ringLogger;

        this.ringLogger = ringLogger;

        if (ringLogger != null) {
            ringLogger.evtUpdate().subEvent(this).setExecutor(UiExecutor.getDefault()).register(this);
        }

        if (prvLogger != null) {
            prvLogger.evtUpdate().clear(this);
        }

        updateLogList();
        updateLogView();

        return this;
    }

    public LogRecyclerViewHelper setColor(int logLevel, @ColorInt int color) {
        colorMap.put(logLevel, color);
        return this;
    }

    public @ColorInt
    int getColor(int logLevel) {
        return colorMap.get(logLevel, Color.BLACK);
    }

    public LogRecyclerViewHelper attachView(RecyclerView logRecycleView) {
        if (logRecycleView != null) {
            logAdapter = new LogRecyclerViewHelper.LogAdapter();
            this.logRecycleView = logRecycleView;
            this.logRecycleView.setLayoutManager(new LinearLayoutManager(logRecycleView.getContext()));
            this.logRecycleView.setAdapter(logAdapter);
            this.logRecycleView.addOnScrollListener(this); // 实现自动滑动到底部的功能

            // 当调用 Adapter#notifyDataSetChanged() 的时候，会移除全部item但默认只回收5个，所以改为20 提高复用率
            this.logRecycleView.getRecycledViewPool().setMaxRecycledViews(0, 20);
        }
        return this;
    }

    public LogRecyclerViewHelper attachView(@NonNull ViewGroup container,
                                            @IdRes int recycleView,
                                            @IdRes int logLevelSpinner,
                                            @IdRes int saveBtn,
                                            @IdRes int clearBtn,
                                            @IdRes int autoScrollBtn,
                                            @IdRes int searchEd) {
        attachView(container.findViewById(recycleView));
        setLogLevelSpinner(container.findViewById(logLevelSpinner));
        setSaveBtn(container.findViewById(saveBtn));
        setClearBtn(container.findViewById(clearBtn));
        setAutoScrollBtn(container.findViewById(autoScrollBtn));
        setSearchEditView(container.findViewById(searchEd));
        return this;
    }

    public LogRecyclerViewHelper setClearBtn(View clearBtn) {
        this.clearBtn = clearBtn;
        if (clearBtn != null) {
            clearBtn.setOnClickListener(this);
        }
        return this;
    }

    public LogRecyclerViewHelper setSaveBtn(View saveBtn) {
        this.saveBtn = saveBtn;
        if (saveBtn != null) {
            saveBtn.setOnClickListener(new DebouncedClickListener(this));
        }
        return this;
    }

    public LogRecyclerViewHelper setAutoScrollBtn(View autoScrollBtn) {
        this.autoScrollBtn = autoScrollBtn;
        if (autoScrollBtn != null) {
            autoScrollBtn.setOnClickListener(this);
        }
        return this;
    }

    public LogRecyclerViewHelper setLogLevelSpinner(Spinner spinner) {
        this.logLevelSpinner = spinner;
        if (spinner != null) {
            spinner.setOnItemSelectedListener(this);
        }
        return this;
    }

    public LogRecyclerViewHelper setSearchEditView(EditText search) {
        this.searchEd = search;
        if (search != null) {
            search.setOnEditorActionListener(this);
        }
        return this;
    }

    public LogRecyclerViewHelper setHideTag(boolean hideTag) {
        this.hideTag = hideTag;
        return this;
    }

    public void setVisibility(int visibility) {
        if (this.logRecycleView != null) {
            this.logRecycleView.setVisibility(visibility);
        }
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public void setLogFilter(String logFilter) {
        if (logFilter != null) {
            this.logFilter = logFilter.trim();
        } else {
            this.logFilter = null;
        }
        updateLogList();
        updateLogView();
    }

    public void setScrollToBottom() {
        scrollToBottom = true;
        if (logRecycleView != null) {
            logRecycleView.scrollToPosition(cleanLogList.size() - 1);
        }
    }

    @Nullable
    public File saveLog(Context ctx) {
        if (ctx != null) {
            File filesDir = ctx.getExternalFilesDir(null);
            String externalStorageState = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(externalStorageState)) {
                return saveLogTo(new File(filesDir, "log"));
            } else {
                Logger.e(ringLogger, TAG, "Failed to save log. ExternalStorageState = " + externalStorageState);
            }
        }
        return null;
    }

    @Nullable
    public File saveLogTo(File logDir) {
        RingLogger logger = this.ringLogger;
        if (logger == null) {
            return null;
        }

        if (!logDir.exists()) {
            if (!logDir.mkdirs()) {
                Logger.e(logger, TAG, "Failed to create dir: " + logDir.getAbsolutePath());
            }
        }
        File logFile = new File(logDir, LogcatUtil.createFileName() + ".log");
        try {
            if (!logFile.exists()) {
                if (!logFile.createNewFile()) {
                    Logger.e(logger, TAG, "Failed to create log file: " + logFile.getAbsolutePath());
                    return null;
                }
            }
            FileWriter writer = new FileWriter(logFile);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            Date date = new Date();
            logger.getLogs(logCopyList);
            for (int i = 0; i < logCopyList.size(); i++) {
                RingLogger.LogEntry entry = logCopyList.get(i);
                // time
                date.setTime(entry.timestamp);
                writer.write(dateFormat.format(date));
                // tid
                writer.write(" ");
                writer.write(String.valueOf(entry.tid));
                // level,tag,message
                writer.write(" ");
                writer.write(String.valueOf(entry.level));
                writer.write(" ");
                writer.write(entry.tag);
                writer.write(": ");
                writer.write(entry.msg);
                writer.write("\n");
            }
            writer.close();
            Logger.i(logger, TAG, "Save log to file: " + logFile.getAbsolutePath());
            return logFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setPauseUpdate(boolean pauseUpdate) {
        if (!pauseUpdate) {
            if (hasUpdateWhilePause) {
                updateLogList();
                updateLogView();
            }
        }
        this.pauseUpdate = pauseUpdate;
        hasUpdateWhilePause = false;
    }

    /**
     * 释放占用的资源
     */
    public void destroy() {
        if (ringLogger != null) {
            ringLogger.evtUpdate().clear(this);
        }

        clearBtn = saveBtn = autoScrollBtn = null;
        logLevelSpinner = null;
        searchEd = null;

        cleanLogList.clear();
        logCopyList.clear();
        if (logAdapter != null) {
            logAdapter.notifyDataSetChanged();
        }
        if (logRecycleView != null) {
            logRecycleView.getRecycledViewPool().clear();
        }
        logAdapter = null;
        logRecycleView = null;
    }

    private void updateLogList() {
        cleanLogList.clear();

        RingLogger logger = this.ringLogger;
        if (logger == null) {
            return;
        }

        logger.getLogs(logCopyList);

        for (int i = 0; i < logCopyList.size(); i++) {
            RingLogger.LogEntry entry = logCopyList.get(i);
            if (entry.level >= logLevel) {
                if (logFilter == null || logFilter.length() == 0
                        || (entry.tag != null && !hideTag && entry.tag.contains(logFilter))
                        || (entry.msg != null && entry.msg.contains(logFilter))) {
                    cleanLogList.add(entry);
                }
            }
        }
    }

    private void updateLogView() {
        if (logRecycleView != null && logAdapter != null) {
            logAdapter.notifyDataSetChanged();
            // 如果用户滑到了底部，就一直保持在底部
            if (!scrollToBottom) {
                scrollToBottom = !logRecycleView.canScrollVertically(1);
            }
            if (scrollToBottom) {
                //logRecycleView.smoothScrollToPosition(stopPos - 1); 会导致性能问题
                logRecycleView.scrollToPosition(cleanLogList.size() - 1);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == clearBtn) {
            if (ringLogger != null) {
                ringLogger.clear();
            }
        } else if (v == saveBtn) {
            saveLog(v.getContext());
        } else if (v == autoScrollBtn) {
            setScrollToBottom();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        setLogLevel(position + Log.VERBOSE);
        updateLogList();
        updateLogView();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        Log.e("---", "onEditorAction() called with: v = [" + v + "], actionId = [" + actionId + "], event = [" + event + "]");
        if (searchEd != null && v == searchEd) {
            setLogFilter(searchEd.getText().toString());
        }
        return true;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        // 如果往回滑动了，就取消自动滑动到底部的功能
        if (dy < 0) {
            scrollToBottom = false;
        }
    }

    @Override
    public void onEvent(Object src, int evtType, Object evtData) {
        if (src == ringLogger) {
            if (pauseUpdate) {
                hasUpdateWhilePause = true;
            } else {
                updateLogList();
                updateLogView();
            }
        }
    }

    private class LogAdapter extends RecyclerView.Adapter<LogRecyclerViewHelper.LogAdapter.LogHolder> {
        private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
        private Date date = new Date();

        @NonNull
        @Override
        public LogRecyclerViewHelper.LogAdapter.LogHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

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
            contentTv.setTextIsSelectable(true);

            root.addView(titleTv);
            root.addView(contentTv);

            return new LogRecyclerViewHelper.LogAdapter.LogHolder(root, titleTv, contentTv);
        }

        @Override
        public void onBindViewHolder(@NonNull LogRecyclerViewHelper.LogAdapter.LogHolder holder, int i) {
            RingLogger.LogEntry entry = cleanLogList.get(i);
            date.setTime(entry.timestamp);
            holder.titleTv.setText(timeFormat.format(date));
            holder.setContent(entry.tag, entry.msg);
            holder.contentTv.setTextColor(getColor(entry.level));
        }

        @Override
        public int getItemCount() {
            return cleanLogList.size();
        }

        private class LogHolder extends RecyclerView.ViewHolder {

            TextView titleTv;
            TextView contentTv;
            HexStringBuilder contentBuilder;

            LogHolder(@NonNull View itemView, TextView titleTv, TextView contentTv) {
                super(itemView);

                this.titleTv = titleTv;
                this.contentTv = contentTv;
                contentBuilder = new HexStringBuilder(128);
            }

            void setContent(String tag, String msg) {
                contentBuilder.clear();
                if (hideTag) {
                    contentBuilder.a(msg);
                } else {
                    contentBuilder.a(tag).a(": ").a(msg);
                }
                this.contentTv.setText(contentBuilder);
            }
        }
    }
}
