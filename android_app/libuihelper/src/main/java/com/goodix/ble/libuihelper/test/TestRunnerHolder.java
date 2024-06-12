package com.goodix.ble.libuihelper.test;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.goodix.ble.libcomx.event.EventDisposer;
import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libcomx.logger.RingLogger;
import com.goodix.ble.libcomx.task.ITask;
import com.goodix.ble.libcomx.task.ITaskResult;
import com.goodix.ble.libcomx.task.ITaskSet;
import com.goodix.ble.libcomx.task.TaskError;
import com.goodix.ble.libuihelper.R;
import com.goodix.ble.libuihelper.input.DebouncedClickListener;
import com.goodix.ble.libuihelper.logger.LogRecyclerViewHelper;
import com.goodix.ble.libuihelper.logger.LogcatUtil;
import com.goodix.ble.libuihelper.thread.UiExecutor;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressWarnings("unused")
@SuppressLint("SetTextI18n")
public class TestRunnerHolder implements View.OnClickListener, Runnable, IEventListener {
    private CheckBox saveLogCb;
    private Button startBtn;
    private TextView startTimeTv;
    private TextView runTimeTv;
    private TextView progressTv;
    private TextView stopTimeTv;
    private TextView statusTv;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private SimpleDateFormat logFileDateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
    private long startTimestamp, stopTimestamp;

    @Nullable
    private ITask targetTestTask;
    private EventDisposer disposer = new EventDisposer();

    private Handler updateTimer = new Handler();

    private LogRecyclerViewHelper logHelper = new LogRecyclerViewHelper();
    private RingLogger testLogger;

    private LinearLayout configLL;
    private LinearLayout statusLL;

    private CB callback;
    private boolean ready = true;

    public interface CB {
        /**
         * 在该回调中将测试界面上的配置参数设置到测试任务中。
         */
        void onConfigBeforeStart(ITask testTask);

        /**
         * 在该回调中更新界面。主要更新测试状态。
         */
        void onUpdateStatus(ITask testTask);
    }

    public TestRunnerHolder(CB callback) {
        this.callback = callback;
    }

    public ITask getTest() {
        return targetTestTask;
    }

    public LinearLayout getConfigLL() {
        return configLL;
    }

    public LinearLayout getStatusLL() {
        return statusLL;
    }

    public View addConfigView(LayoutInflater inflater, @LayoutRes int resource) {
        return addView(inflater, resource, configLL);
    }

    public View addStatusView(LayoutInflater inflater, @LayoutRes int resource) {
        return addView(inflater, resource, statusLL);
    }

    public RingLogger getLogger() {
        return testLogger;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
        if (targetTestTask == null) {
            return;
        }
        if (targetTestTask.isStarted()) {
            return;
        }
        if (startBtn != null) {
            startBtn.setEnabled(ready);
        }
    }

    @NonNull
    public View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.libuihelper_fragment_test_runner, container, false);

        configLL = root.findViewById(R.id.libuihelper_fragment_test_runner_config_ll);
        statusLL = root.findViewById(R.id.libuihelper_fragment_test_runner_status_ll);

        saveLogCb = root.findViewById(R.id.libuihelper_fragment_test_runner_save_log_cb);
        startBtn = root.findViewById(R.id.libuihelper_fragment_test_runner_start_btn);
        startBtn.setOnClickListener(new DebouncedClickListener(this));

        startTimeTv = root.findViewById(R.id.libuihelper_fragment_test_runner_start_time_tv);
        runTimeTv = root.findViewById(R.id.libuihelper_fragment_test_runner_run_time_tv);
        progressTv = root.findViewById(R.id.libuihelper_fragment_test_runner_progress_tv);
        stopTimeTv = root.findViewById(R.id.libuihelper_fragment_test_runner_stop_time_tv);
        statusTv = root.findViewById(R.id.libuihelper_fragment_test_runner_status_tv);

        if (testLogger == null) {
            testLogger = new RingLogger(100);
        }
        logHelper.attachView(root.findViewById(R.id.libuihelper_fragment_test_runner_log_rv))
                .setRingLogger(testLogger);

        return root;
    }

    public void setTest(ITask testTask) {
        targetTestTask = testTask;
        if (targetTestTask == null) {
            throw new NullPointerException("Test task is null.");
        } else {
            // 如果测试已经运行，就恢复显示
            if (targetTestTask.isStarted()) {
                testLogger.v(getClass().getSimpleName(), "恢复显示测试过程");
                startTest();
            }
        }
        if (targetTestTask.getLogger() == null) {
            targetTestTask.setLogger(testLogger);
        }
    }

    public void destroy() {
        disposer.disposeAll(null);
    }

    @Override
    public void onClick(View v) {
        if (v == startBtn) {
            if (ready) {
                if (targetTestTask != null) {
                    if (targetTestTask.isStarted()) {
                        targetTestTask.abort();
                    } else {
                        startTest();
                    }
                } else {
                    testLogger.e(getClass().getSimpleName(), "Can't start test. Test is null.");
                }
            } else {
                testLogger.i(getClass().getSimpleName(), "Not ready to start test.");
            }
        }
    }

    private void startTest() {
        if (targetTestTask == null) return;

        targetTestTask.evtStart()
                .setDisposer(disposer).setExecutor(UiExecutor.getDefault()).register(this);

        targetTestTask.evtProgress()
                .setDisposer(disposer).setExecutor(UiExecutor.getDefault()).register(this);

        targetTestTask.evtFinished()
                .setDisposer(disposer).setExecutor(UiExecutor.getDefault()).register(this);

        if (targetTestTask instanceof ITaskSet) {
            // 监听case的step
            ((ITaskSet) targetTestTask).evtSubtaskStart()
                    .setDisposer(disposer).setExecutor(UiExecutor.getDefault()).register(this);
            ((ITaskSet) targetTestTask).evtSubtaskFinish()
                    .setDisposer(disposer).setExecutor(UiExecutor.getDefault()).register(this);
        }

        statusTv.setText(R.string.libuihelper_na);
        progressTv.setText(R.string.libuihelper_na);

        startTimestamp = System.currentTimeMillis();
        startTimeTv.setText(dateFormat.format(new Date(startTimestamp)));

        stopTimestamp = 0;
        stopTimeTv.setText(R.string.libuihelper_na);

        if (targetTestTask.isStarted()) {
            enableUI(false);
            startTimeTv.setText(dateFormat.format(new Date(startTimestamp)));

        } else {
            testLogger.clearSync();
            // 配置测试任务
            if (callback != null) {
                callback.onConfigBeforeStart(targetTestTask);
            }
            targetTestTask.setExecutor(UiExecutor.getDefault());
            targetTestTask.setLogger(testLogger);
            targetTestTask.start(null, null);
        }

        updateTimer.post(this);
    }


    @Override
    public void onEvent(Object src, int evtType, Object evtData) {
        if (src == targetTestTask) {
            switch (evtType) {
                case ITask.EVT_START: {
                    enableUI(false);
                    break;
                }
                case ITask.EVT_PROGRESS: {
                    progressTv.setText(evtData + "%");
                    break;
                }
                case ITaskSet.EVT_SUBTASK_START: {
                    ITask caseTask = (ITask) evtData;

                    statusTv.setText(caseTask.getName());
                    testLogger.i("Case", caseTask.getName());

                    // 监听step
                    if (caseTask instanceof ITaskSet) {
                        ((ITaskSet) caseTask).evtSubtaskStart()
                                .setExecutor(UiExecutor.getDefault()).register(this);
                        caseTask.evtFinished()
                                .setExecutor(UiExecutor.getDefault()).register(this);
                    }
                    break;
                }
                case ITaskSet.EVT_SUBTASK_FINISH: {
                    ITask caseTask = (ITask) evtData;
                    TaskError error = caseTask.getResult().getError();
                    if (error != null) {
                        //statusTv.setText(error.getMessage());
                        testLogger.e("status", error.getRootCause().getMessage());
                    }
                    break;
                }
                case ITask.EVT_FINISH: {
                    ITaskResult result = (ITaskResult) evtData;
                    TaskError error = result.getError();

                    stopTimestamp = System.currentTimeMillis();

                    if (error != null) {
                        statusTv.setText(error.getMessage());
                        testLogger.e("status", error.getRootCause().getMessage());

                        if (saveLogCb.isChecked()) {
                            try {
                                final File dir = new File(saveLogCb.getContext().getExternalFilesDir(null), "test");
                                final File testCaseDir = new File(dir, logFileDateFormat.format(new Date(startTimestamp)));
                                testLogger.saveTo(new File(testCaseDir, "TestLogger.log"));
                                if (LogcatUtil.saveAndroidLog(new File(testCaseDir, "Logcat.log")) != null) {
                                    testLogger.i("status", "已保存Logcat日志");
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        statusTv.setText("测试完成");
                        testLogger.i("status", "测试完成");
                    }

                    enableUI(true);
                    disposer.disposeAll(null);

                    runTimeTv.setText(((stopTimestamp - startTimestamp) / 1000) + "秒");
                    stopTimeTv.setText(dateFormat.format(new Date(stopTimestamp)));
                    progressTv.setText(R.string.libuihelper_na);

                    if (callback != null) {
                        callback.onUpdateStatus(targetTestTask);
                    }
                    break;
                }
            }
        } else {
            // step and actions
            // 子步骤的子步骤
            if (evtType == ITaskSet.EVT_SUBTASK_START && src instanceof ITask) {
                ITaskSet caseTask = (ITaskSet) src; // case
                ITask stepTask = (ITask) evtData; // step
                showSteps(caseTask, stepTask, caseTask.getName());
            } else if (evtType == ITaskSet.EVT_FINISH && src instanceof ITask) {
                ((ITaskSet) src).evtSubtaskStart().clear();
            }
        }
    }

    private void showSteps(ITaskSet stepTask, ITask actionTask, String parentPath) {
        if (parentPath == null) {
            parentPath = stepTask.getName();
        }
        String childName = parentPath + " -> " + actionTask.getName();
        statusTv.setText(childName);
        testLogger.i("Step", childName);

        // 监听step的action
        if (actionTask instanceof ITaskSet) {
            ((ITaskSet) actionTask).evtSubtaskStart()
                    .setExecutor(UiExecutor.getDefault()).register2((src, evtType, evtData) -> {
                // step -> action
                showSteps((ITaskSet) src, evtData, childName);
            });
            actionTask.evtFinished()
                    .setExecutor(UiExecutor.getDefault()).register(this);
        }
    }

    @Override
    public void run() {
        if (targetTestTask != null) {
            if (targetTestTask.isStarted()) {
                runTimeTv.setText(((System.currentTimeMillis() - startTimestamp) / 1000) + "秒");
                updateTimer.removeCallbacks(this);
                updateTimer.postDelayed(this, 1000);
            }

            if (callback != null) {
                callback.onUpdateStatus(targetTestTask);
            }
        }
    }

    private void enableUI(boolean enable) {
        //containerLL.setEnabled(enable);
        enableViewGroup(configLL, enable);
        startBtn.setText(enable ? "开始测试" : "停止测试");
        if (enable) {
            startBtn.setEnabled(ready);
        }
    }

    private void enableViewGroup(ViewGroup container, boolean enable) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof ViewGroup) {
                enableViewGroup((ViewGroup) child, enable);
            } else {
                child.setEnabled(enable);
            }
        }
    }

    private View addView(LayoutInflater inflater, @LayoutRes int resource, ViewGroup container) {
        if (container == null) {
            throw new NullPointerException("Please call createView() first.");
        }

        View root = inflater.inflate(resource, container, false);
        container.addView(root);

        return root;
    }
}
