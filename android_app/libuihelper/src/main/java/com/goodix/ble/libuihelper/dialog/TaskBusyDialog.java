package com.goodix.ble.libuihelper.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libcomx.task.ITask;
import com.goodix.ble.libcomx.task.ITaskResult;
import com.goodix.ble.libcomx.task.TaskError;
import com.goodix.ble.libuihelper.R;
import com.goodix.ble.libuihelper.thread.UiExecutor;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class TaskBusyDialog extends DialogFragment implements IEventListener, View.OnClickListener, Runnable {
    private FragmentActivity host;
    private Fragment parentFragment;

    private boolean oneshot = false;
    private boolean autoDismiss = false;
    private boolean ignoreError = false;
    private boolean showRawErrorMessage = false;
    private int delay = 200;
    private Handler delayTimer;

    protected ITask task;
    protected String title = null;
    private ITaskResult remainTaskResult = null; // 用于在使用oneshot的情况下保存最后的错误信息。

    //@Nullable
    protected View root;
    //@Nullable
    protected View cancelBtn; // Defined as View for generic usage. If it is a TextView, it's text would be modified
    //@Nullable
    protected TextView titleTv;
    //@Nullable
    protected TextView tipTv;
    //@Nullable
    protected View progressBar; // Defined as View for generic usage. Only it's visibility would be modified.

    protected float windowWidthPercent = 0.6f;
    protected int textColorNormal = Color.BLACK;
    protected int textColorSuccess = Color.BLACK;
    protected int textColorError = Color.RED;
    @StringRes
    protected int textWait = R.string.libuihelper_please_wait;
    @StringRes
    protected int textSuccess = R.string.libuihelper_complete;
    @StringRes
    protected int textOk = android.R.string.ok;
    @StringRes
    protected int textAbort = R.string.libuihelper_abort;

    private boolean requestClosing = false;
    private int state = ST_IDLE;
    private static final int ST_IDLE = 0;
    private static final int ST_PENDING = 1;  // 延时等待的状态，让Task有机会在显示对话框之前结束。
    private static final int ST_CREATING = 2; // 已经提交了显示界面的请求，等待系统准备好资源。
    private static final int ST_SHOWING = 3; // 已处于可见的显示状态，等待任务结束或用户中止。
    private static final int ST_CLOSING = 4; // 提交了关闭界面的请求，等待系统释放资源。

    public TaskBusyDialog setHost(FragmentActivity host) {
        this.host = host;
        this.parentFragment = null;
        return this;
    }

    public TaskBusyDialog setHost(Fragment parent) {
        this.parentFragment = parent;
        this.host = null;
        return this;
    }

    public TaskBusyDialog setDelay(int delay) {
        this.delay = delay;
        return this;
    }

    public TaskBusyDialog setTitle(String title) {
        this.title = title;

        TextView titleTv = this.titleTv;
        if (titleTv != null) {
            if (title == null) {
                titleTv.setVisibility(View.GONE);
            } else {
                titleTv.setVisibility(View.VISIBLE);
                titleTv.setText(title);
            }
        }
        return this;
    }

    public TaskBusyDialog setOneshot() {
        this.oneshot = true;
        return this;
    }

    public TaskBusyDialog setAutoDismiss(boolean autoDismiss) {
        this.autoDismiss = autoDismiss;
        return this;
    }

    public TaskBusyDialog setIgnoreError(boolean ignoreError) {
        this.ignoreError = ignoreError;
        return this;
    }

    public TaskBusyDialog setShowRawErrorMessage(boolean showRawErrorMessage) {
        this.showRawErrorMessage = showRawErrorMessage;
        return this;
    }

    public TaskBusyDialog setOnDismissListener(DialogInterface.OnDismissListener listener) {
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setOnDismissListener(listener);
        }
        return this;
    }

    public TaskBusyDialog bind(ITask task) {
        if (state == ST_CLOSING) {
            throw new IllegalStateException("Unable to bind task with a destructing UI.");
        }

        unbind(); // 先移除已有的绑定

        this.task = task;
        if (task != null) {
            task.evtFinished()
                    .subEvent(this).setExecutor(UiExecutor.getDefault()).register(this);
            task.evtStart()
                    .subEvent(this).setExecutor(UiExecutor.getDefault()).register(this);

            if (task.isStarted()) {
                onEvent(task, ITask.EVT_START, null); // 直接触发开始显示
            }
        }
        return this;
    }

    public TaskBusyDialog unbind() {
        ITask task = this.task;
        this.task = null;
        if (task != null) {
            task.evtStart()
                    .clear(this);
            task.evtFinished()
                    .clear(this);
        }
        return this;
    }

    public void show() {
        startTask();
    }

    public void startTask() {
        ITask task = this.task;
        if (task != null) {
            task.start(null, null);
        }
    }

    @Override
    @Deprecated
    public int show(@NonNull FragmentTransaction transaction, @Nullable String tag) {
        startTask();
        return 0;
    }

    @Override
    @Deprecated
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        startTask();
    }

    @Override
    public void onStart() {
        super.onStart();

        final Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                WindowManager.LayoutParams lp = window.getAttributes();
                lp.width = (int) (displayMetrics.widthPixels * windowWidthPercent);
                if (lp.width > displayMetrics.widthPixels) {
                    lp.width = displayMetrics.widthPixels;
                } else if (lp.width <= 0) {
                    lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                //lp.height = lp.width / 2;
                //lp.height = displayMetrics.heightPixels;
                window.setAttributes(lp);
            }
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
        }

        state = ST_SHOWING;
        //Log.e("+++++", "ST_SHOWING");
    }

    @Override
    public void onResume() {
        super.onResume();

        //  如果出现了极端条件，取消了显示，但是Fragment又进入了初始化流程，那么就直接取消Fragment的显示。
        if (requestClosing) {
            dismiss();
        } else {
            ITask task = this.task;
            if (task != null) {
                ITaskResult result = task.getResult();
                if (task.isFinished() && result != null) {
                    checkResult(result);
                }
            } else if (remainTaskResult != null) {
                onShowResult(remainTaskResult);
            } else {
                // task都不存在了，就关闭显示
                if (autoDismiss) {
                    dismiss();
                } else {
                    onShowResult(null); // 显示关闭
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ITask task = this.task;
        if (task != null && task.isStarted()) {
            task.abort();
        }

        if (oneshot) {
            host = null;
            parentFragment = null;

            unbind();
        }

        remainTaskResult = null; // 解除引用
        state = ST_IDLE;
        //Log.e("+++++", "ST_IDLE");
    }

    @Override
    public void onEvent(Object src, int evtType, Object evtData) {
        if (src == task) {
            if (evtType == ITask.EVT_START) {
                remainTaskResult = null; // 清空，便于保存这次运行的错误信息
                if (state == ST_IDLE) {
                    state = ST_PENDING;
                    //Log.e("+++++", "ST_PENDING");
                    if (delayTimer == null) {
                        delayTimer = new Handler(Looper.getMainLooper());
                    }
                    if (delay > 0) {
                        delayTimer.postDelayed(this, delay);
                    } else {
                        this.run();
                    }
                } else if (state == ST_SHOWING) {
                    onRestUI();
                }

            } else if (evtType == ITask.EVT_FINISH) {
                ITaskResult result = (ITaskResult) evtData;

                if (oneshot) {
                    unbind();
                }

                checkResult(result);
            }
        }
    }

    @Override
    public void dismiss() {
        //Log.e("+++++", "dismiss: " + CallUtil.trace(3));
        switch (state) {
            case ST_IDLE:
            case ST_CLOSING:
                break;
            case ST_PENDING:
                state = ST_IDLE;
                if (delayTimer != null) {
                    delayTimer.removeCallbacksAndMessages(null);
                }
                break;
            case ST_CREATING:
                requestClosing = true;
                break;
            default: // ST_SHOWING
                state = ST_CLOSING;
                super.dismiss();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == this.cancelBtn) {
            dismiss();
        }
    }

    @Override
    public void run() {
        if (state == ST_PENDING) {
            state = ST_IDLE;
            //Log.e("+++++", "ST_CREATING");
            if (host != null) {
                super.show(host.getSupportFragmentManager(), this.getClass().getSimpleName());
                state = ST_CREATING;
                requestClosing = false;
            } else if (parentFragment != null) {
                super.show(parentFragment.getChildFragmentManager(), this.getClass().getSimpleName());
                state = ST_CREATING;
                requestClosing = false;
            }
        }
    }

    private void checkResult(ITaskResult result) {
        //Log.e("+++++", "checkResult: " + CallUtil.trace(3));
        if (state == ST_SHOWING && this.root != null) {
            // 如果有视图就直接刷新，没有就等待界面显示出来后由onResume()刷新。
            onShowResult(result);
        } else if (state == ST_PENDING) {
            // 界面还没有显示
            // 没有错误或忽略错误时，就直接终止显示
            if (result.getError() == null || ignoreError) {
                dismiss();
            } else {
                // 否则让onResume()来显示结果
                if (oneshot) {
                    // oneshot时，因为对task的引用会被清空，所以用remainTaskResult来保存错误信息
                    remainTaskResult = result;
                }
                // 取消延时，立即显示
                if (delayTimer != null) {
                    delayTimer.removeCallbacksAndMessages(null);
                }
                this.run();
            }
        }

        // 如果有自动关闭界面的标志位，那么，就在没有出错时直接关闭界面。
        if (autoDismiss) {
            if (result.getError() == null || ignoreError) {
                dismiss();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.libuihelper_dialog_task_busy, container, false);
        cancelBtn = root.findViewById(R.id.libuihelper_cancel_btn);
        titleTv = root.findViewById(R.id.libuihelper_title_tv);
        tipTv = root.findViewById(R.id.libuihelper_tip_tv);
        progressBar = root.findViewById(R.id.libuihelper_progress_bar);

        if (this.cancelBtn != null) {
            this.cancelBtn.setOnClickListener(this);
        }

        onRestUI();
        return root;
    }

    protected void onRestUI() {
        if (this.cancelBtn != null) {
            this.cancelBtn.setVisibility(View.VISIBLE);
        }
        if (this.cancelBtn instanceof TextView) {
            ((TextView) this.cancelBtn).setText(textAbort);
        }

        if (this.tipTv != null) {
            this.tipTv.setText(textWait);
            if (textColorNormal != 0) {
                this.tipTv.setTextColor(textColorNormal);
            }
        }

        if (this.progressBar != null) {
            this.progressBar.setVisibility(View.VISIBLE);
        }

        if (this.titleTv != null) {
            if (title == null) {
                this.titleTv.setVisibility(View.GONE);
            } else {
                this.titleTv.setVisibility(View.VISIBLE);
                this.titleTv.setText(title);
            }
        }
    }

    /**
     * update UI to show the result of task.
     *
     * @param result the value may be null if the task is not existed. if it is null, show completion.
     */
    protected void onShowResult(@Nullable ITaskResult result) {
        if (this.progressBar != null) {
            this.progressBar.setVisibility(View.GONE);
        }
        if (this.cancelBtn != null) {
            this.cancelBtn.setVisibility(View.VISIBLE);
            if (this.cancelBtn instanceof TextView) {
                ((TextView) this.cancelBtn).setText(textOk);
            }
        }
        if (result != null && result.getError() != null) {
            if (this.tipTv != null) {
                String errMsg;
                Throwable rootCause = result.getError().getRootCause();
                if (showRawErrorMessage && rootCause instanceof TaskError) {
                    errMsg = ((TaskError) rootCause).getRawMessage();
                } else {
                    errMsg = rootCause.getMessage();
                }
                this.tipTv.setText(this.tipTv.getContext().getString(R.string.libuihelper_err_msg, errMsg));
                if (textColorError != 0) {
                    this.tipTv.setTextColor(textColorError);
                }
            }
        } else {
            if (this.tipTv != null) {
                this.tipTv.setText(textSuccess);
                if (textColorSuccess != 0) {
                    this.tipTv.setTextColor(textColorSuccess);
                }
            }
        }
    }
}
