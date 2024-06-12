package com.goodix.ble.libuihelper.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libcomx.task.ITask;
import com.goodix.ble.libcomx.task.ITaskContext;
import com.goodix.ble.libcomx.task.ITaskResult;
import com.goodix.ble.libcomx.util.HexStringBuilder;
import com.goodix.ble.libuihelper.R;
import com.goodix.ble.libuihelper.input.DebouncedClickListener;
import com.goodix.ble.libuihelper.thread.UiExecutor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Executor;

public class TaskIndicatorDialog implements ITaskContext {

    private final Context mCtx;
    private final AlertDialog dialog;
    private final LinearLayout msgLayout;
    private final LinearLayout btnBarLayout;
    private final ProgressBar progressBar;
    private final Button clearBtn;
    private final Button closeBtn;
    private final Button abortBtn;

    private LinkedList<Holder> taskList = new LinkedList<>();

    private final UiExecutor executor;
    private final Event<Void> startEvent;
    private final Event<Integer> progressEvent;
    private final Event<ITaskResult> finisEvent;
    private HashMap<String, Object> parameters = new HashMap<>();
    private boolean allComplete = false;

    private int textColorForComplete;
    private int textColorForError;

    public TaskIndicatorDialog(Context ctx) {
        mCtx = ctx;
        @SuppressLint("InflateParams")
        View root = LayoutInflater.from(ctx).inflate(R.layout.libuihelper_dialog_task_indicator, null);

        msgLayout = root.findViewById(R.id.libuihelper_dialog_task_indicator_container_ll);
        btnBarLayout = root.findViewById(R.id.libuihelper_dialog_task_indicator_btn_bar_ll);
        progressBar = root.findViewById(R.id.libuihelper_dialog_task_indicator_progress_bar);
        clearBtn = root.findViewById(R.id.libuihelper_dialog_task_indicator_clear_btn);
        closeBtn = root.findViewById(R.id.libuihelper_dialog_task_indicator_close_btn);
        abortBtn = root.findViewById(R.id.libuihelper_dialog_task_indicator_abort_btn);

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(R.string.libuihelper_title_task_dialog);
        builder.setView(root);
        builder.setCancelable(false); // 只能点击Close去关闭
        dialog = builder.create();

        DebouncedClickListener clickListener = new DebouncedClickListener(v -> {
            if (v == closeBtn) {
                dialog.dismiss();
            } else if (v == clearBtn) {
                if (!taskList.isEmpty()) {
                    Holder holder = taskList.peekFirst();
                    if (holder != null) {
                        holder.task.abort();
                    }
                    while (taskList.size() > 1) {
                        holder = taskList.removeLast();
                        holder.task.abort();
                        holder.state.put(mCtx.getString(R.string.libuihelper_cancel));
                        holder.stateTv.setText(holder.state);
                    }
                }
            } else if (v == abortBtn) {
                if (!taskList.isEmpty()) {
                    Holder holder = taskList.peekFirst();
                    if (holder != null) {
                        holder.task.abort();
                    }
                }
            }
        });
        clearBtn.setOnClickListener(clickListener);
        clearBtn.setVisibility(View.GONE);
        closeBtn.setOnClickListener(clickListener);
        abortBtn.setOnClickListener(clickListener);
        abortBtn.setVisibility(View.GONE);

        executor = new UiExecutor();
        startEvent = new Event<>();
        startEvent.setExecutor(executor);
        startEvent.register2((src, type, child) -> {
            Holder holder = taskList.peekFirst();
            if (holder != null && holder.task == src) {
                holder.state.clear();
                holder.state.put(mCtx.getString(R.string.libuihelper_start));
                holder.stateTv.setText(holder.state);
            }
        });

        progressEvent = new Event<>();
        progressEvent.setExecutor(executor);
        progressEvent.register((src, type, percent) -> {
            Holder holder = taskList.peekFirst();
            if (holder != null) {
                holder.state.clear();
                if (holder.task == src) {
                    holder.state.put(mCtx.getString(R.string.libuihelper_running)).format(" %d%%", percent);
                } else {
                    ITask childTask = (ITask) src;
                    holder.state.put(childTask.getName()).format(" %d%%", percent);
                }
                holder.stateTv.setText(holder.state);
            }
        });

        finisEvent = new Event<>();
        finisEvent.setExecutor(executor);
        finisEvent.register2((src, type, result) -> {
            if (!taskList.isEmpty()) {
                Holder holder = taskList.removeFirst();
                // 当前任务显示错误信息
                if (holder != null && holder.task == src) {
                    if (result.getError() == null) {
                        holder.state.clear().put(mCtx.getString(R.string.libuihelper_complete));
                    } else {
                        holder.state.clear().put(mCtx.getString(R.string.libuihelper_error)).put(": ").put(result.getError().getMessage());
                        if (result.getError().getCause() != null) {
                            holder.state.append("Caused by: ").put(result.getError().getCause().getMessage());
                        }
                        holder.stateTv.setTextColor(Color.RED);
                    }
                    holder.stateTv.setText(holder.state);
                }
                // 启动下一个任务
                startNextTask();
            }
        });
    }

    private void startNextTask() {
        Holder holder;
        holder = taskList.peekFirst();
        IEventListener all = new IEventListener() {
            @Override
            public void onEvent(Object src, int type, Object evtData) {
                //
            }
        };
        startEvent.register(all);
        progressEvent.register(all);
        finisEvent.register(all);
        if (holder != null) {
            holder.task.evtStart().register(startEvent);
            holder.task.evtProgress().register(progressEvent);
            holder.task.evtFinished().register(finisEvent);
            holder.task.start(this, null);

            if (progressBar.getVisibility() != View.VISIBLE) {
                progressBar.setVisibility(View.VISIBLE);
            }
        } else {
            allComplete = true;
            progressBar.setVisibility(View.INVISIBLE); // 因为进度条的显示和隐藏，导致界面跳动
        }
    }

    public void setShowClear() {
        clearBtn.setVisibility(View.VISIBLE);
    }

    public void setShowAbort() {
        abortBtn.setVisibility(View.VISIBLE);
    }

    public void close() {
        closeBtn.performClick();
    }

    public void show() {
        if (!dialog.isShowing()) {
            dialog.show();
        }
    }

    public void setTextColorForComplete(int textColorForComplete) {
        this.textColorForComplete = textColorForComplete;
    }

    public int getTextColorForError() {
        return textColorForError;
    }

    public void addTask(ITask task) {
        addTask(task, null);
    }

    public void addTask(ITask task, int resId) {
        addTask(task, mCtx.getResources().getString(resId));
    }

    public void addTask(ITask task, String name) {
        if (task == null) {
            return;
        }

        if (allComplete) {
            msgLayout.removeAllViews();
        }
        allComplete = false;

        Holder holder = new Holder();
        taskList.add(holder);

        holder.task = task;
        holder.nameTv = new TextView(mCtx);
        holder.nameTv.setText(name != null ? name : task.getName());
        holder.stateTv = new TextView(mCtx);
        holder.stateTv.setPadding(30, 0, 0, 10);
        holder.state = new HexStringBuilder();
        holder.state.put("Waiting...");
        holder.stateTv.setText(holder.state);
        msgLayout.addView(holder.nameTv);
        msgLayout.addView(holder.stateTv);

        if (!dialog.isShowing()) {
            dialog.show();
        }

        if (taskList.size() == 1) {
            startNextTask();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getParameter(String key) {
        return (T) parameters.get(key);
    }

    @Override
    public <T> void setParameter(String key, T val) {
        parameters.put(key, val);
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    private static class Holder {
        ITask task;
        TextView nameTv;
        TextView stateTv;
        HexStringBuilder state;
    }
}
