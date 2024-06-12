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

import androidx.annotation.Nullable;

import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libcomx.task.ITask;
import com.goodix.ble.libcomx.task.ITaskResult;
import com.goodix.ble.libcomx.task.TaskPipe;
import com.goodix.ble.libcomx.util.HexStringBuilder;
import com.goodix.ble.libuihelper.R;
import com.goodix.ble.libuihelper.input.DebouncedClickListener;
import com.goodix.ble.libuihelper.thread.UiExecutor;

import java.util.ArrayList;

// TODO: 2020/3/16 使用TaskItem作为管理的基本单位，使用一个完成列表来展示已近完成的任务，使用一个pipe列表来和TaskPipe中的列表同步，让后再将这两个列表展示在界面上
@SuppressWarnings("unused")
public class TaskPipeDialog implements IEventListener {

    private final Context mCtx;
    private final AlertDialog dialog;
    private final LinearLayout msgLayout;
    private final LinearLayout btnBarLayout;
    private final ProgressBar progressBar;
    private final Button clearBtn;
    private final Button closeBtn;
    private final Button abortBtn;

    @Nullable
    private TaskPipe taskPipe;

    private ArrayList<Holder> holderList = new ArrayList<>(32);

    private int textColorForComplete = Color.BLUE;
    private int textColorForError = Color.RED;
    private boolean autoDismiss = false;

    public TaskPipeDialog(Context ctx) {
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
                if (taskPipe != null && !taskPipe.isBusy()) {
                    msgLayout.removeAllViews();
                    holderList.clear();
                }
            } else if (v == clearBtn) {
                if (taskPipe != null) {
                    taskPipe.clearTask();
                }
                // 清除全部进度订阅
                for (Holder holder : holderList) {
                    if (!holder.finished) {
                        holder.task.evtProgress().clear(this);
                    }
                }
                holderList.clear();

                // 最后才移除View
                msgLayout.removeAllViews();
            } else if (v == abortBtn) {
                if (taskPipe != null) {
                    taskPipe.abortTask();
                }
            }
        });
        clearBtn.setOnClickListener(clickListener);
        clearBtn.setVisibility(View.GONE);
        closeBtn.setOnClickListener(clickListener);
        abortBtn.setOnClickListener(clickListener);
        abortBtn.setVisibility(View.GONE);
    }

    public TaskPipeDialog setTaskPipe(@Nullable TaskPipe taskPipe) {
        final TaskPipe prePipe = this.taskPipe;
        this.taskPipe = taskPipe;

        if (prePipe != null) {
            prePipe.evtBusy().clear(this);
            prePipe.evtTaskAdded().clear(this);
            prePipe.evtTaskStart().clear(this);
            prePipe.evtTaskRemoved().clear(this);
        }

        if (taskPipe != null) {
            taskPipe.evtBusy().subEvent(this).setExecutor(UiExecutor.getDefault()).register(this);
            taskPipe.evtTaskAdded().subEvent(this).setExecutor(UiExecutor.getDefault()).register(this);
            taskPipe.evtTaskStart().subEvent(this).setExecutor(UiExecutor.getDefault()).register(this);
            taskPipe.evtTaskRemoved().subEvent(this).setExecutor(UiExecutor.getDefault()).register(this);
        }
        return this;
    }

    public TaskPipe getTaskPipe() {
        TaskPipe taskPipe = this.taskPipe;
        if (taskPipe == null) {
            throw new IllegalStateException("task pipe is null.");
        }
        return taskPipe;
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

    public void setAutoDismiss(boolean autoDismiss) {
        this.autoDismiss = autoDismiss;
    }

    public void destroy() {
        // 先取消对事件的订阅
        final TaskPipe pipe = this.taskPipe;
        if (pipe != null) {
            pipe.evtBusy().clear(this);
            pipe.evtTaskAdded().clear(this);
            pipe.evtTaskStart().clear(this);
            pipe.evtTaskRemoved().clear(this);
        }

        // 清除全部进度订阅
        for (Holder holder : holderList) {
            if (!holder.finished) {
                holder.task.evtProgress().clear(this);
            }
        }
        holderList.clear();

        // 最后才移除View
        msgLayout.removeAllViews();
    }

    private void addTask(TaskPipe.TaskItem item) {
        if (item == null) {
            return;
        }

        Holder holder = new Holder();
        holderList.add(holder);

        holder.task = item.task;
        holder.finished = false;

        holder.nameTv = new TextView(mCtx);
        holder.nameTv.setText(item.name);

        holder.stateTv = new TextView(mCtx);
        holder.stateTv.setPadding(30, 0, 0, 10);
        holder.stateTv.setText(holder.state);

        holder.state = new HexStringBuilder();
        holder.state.put("Waiting...");

        msgLayout.addView(holder.nameTv);
        msgLayout.addView(holder.stateTv);
    }

    @Override
    public void onEvent(Object src, int evtType, Object evtData) {
        if (src != taskPipe) {
            return;
        }

        if (evtType == TaskPipe.EVT_TASK_ADDED) {
            TaskPipe.TaskItem it = (TaskPipe.TaskItem) evtData;

            addTask(it);

            if (!dialog.isShowing()) {
                dialog.show();
            }
        } else if (evtType == TaskPipe.EVT_BUSY) {
            Boolean busy = (Boolean) evtData;
            progressBar.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);

        } else if (evtType == TaskPipe.EVT_TASK_START) {
            TaskPipe.TaskItem item = (TaskPipe.TaskItem) evtData;

            for (Holder holder : holderList) {
                if (!holder.finished && holder.task == item.task) {
                    // 更新启动状态
                    holder.state.clear();
                    holder.state.put(mCtx.getString(R.string.libuihelper_start));
                    holder.stateTv.setText(holder.state);
                    // 订阅进度更新
                    item.task.evtProgress().subEvent(this)
                            .setExecutor(UiExecutor.getDefault())
                            .register((s, t, percent) -> {
                                holder.state.clear();
                                if (holder.task == s) {
                                    holder.state.put(mCtx.getString(R.string.libuihelper_running))
                                            .format(" %d%%", percent);
                                } else {
                                    ITask childTask = (ITask) s;
                                    holder.state.put(mCtx.getString(R.string.libuihelper_running))
                                            .put(" ")
                                            .put(childTask.getName()).format(" %d%%", percent);
                                }
                                holder.stateTv.setText(holder.state);
                            });
                }
            }
        } else if (evtType == TaskPipe.EVT_TASK_REMOVED) {
            TaskPipe.TaskItem item = (TaskPipe.TaskItem) evtData;

            for (Holder holder : holderList) {
                // 当前任务显示错误信息
                if (!holder.finished && holder.task == item.task) {
                    holder.finished = true;
                    item.task.evtProgress().clear(this); // 清除进度订阅
                    final ITaskResult result = item.result;
                    if (result.getError() == null) {
                        holder.stateTv.setTextColor(textColorForComplete);
                        holder.state.clear().put(mCtx.getString(R.string.libuihelper_complete));
                    } else {
                        holder.stateTv.setTextColor(textColorForError);
                        holder.state.clear().put(mCtx.getString(R.string.libuihelper_error)).put(": ").put(result.getError().getMessage());
                        if (result.getError().getCause() != null) {
                            holder.state.append("Caused by: ").put(result.getError().getCause().getMessage());
                        }
                    }
                    holder.stateTv.setText(holder.state);
                }
            }

            if (autoDismiss) {
                boolean noError = true;
                boolean allDone = true;
                for (Holder holder : holderList) {
                    if (holder.finished) {
                        if (holder.task.getResult().getError() != null) {
                            noError = false;
                            break;
                        }
                    } else {
                        allDone = false;
                        break;
                    }
                }

                if (noError && allDone && taskPipe != null) {
                    msgLayout.removeAllViews();
                    holderList.clear();
                    dialog.dismiss();
                }
            }
        }
    }

    private static class Holder {
        ITask task;
        boolean finished;
        TextView nameTv;
        TextView stateTv;
        HexStringBuilder state;
    }
}
