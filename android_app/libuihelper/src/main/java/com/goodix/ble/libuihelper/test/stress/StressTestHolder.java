package com.goodix.ble.libuihelper.test.stress;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.goodix.ble.libcomx.event.EventDisposer;
import com.goodix.ble.libcomx.logger.RingLogger;
import com.goodix.ble.libcomx.task.TaskError;
import com.goodix.ble.libuihelper.R;
import com.goodix.ble.libuihelper.input.DebouncedClickListener;
import com.goodix.ble.libuihelper.logger.Log;
import com.goodix.ble.libuihelper.logger.LogRecyclerViewHelper;
import com.goodix.ble.libuihelper.logger.LogcatUtil;
import com.goodix.ble.libuihelper.sublayout.ValueEditorHolder;
import com.goodix.ble.libuihelper.thread.UiExecutor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressLint("SetTextI18n")
public class StressTestHolder implements View.OnClickListener, Runnable {
    private CheckBox statusLogCb;
    private CheckBox saveLogCb;
    private CheckBox alarmCb;
    private Button startBtn;
    private ValueEditorHolder totalCountHolder;
    private ValueEditorHolder maxFailHolder;
    private ValueEditorHolder maxTimeHolder;
    private ValueEditorHolder maxHardFailHolder;
    private TextView startTimeTv;
    private TextView runTimeTv;
    private TextView runCountTv;
    private TextView failCountTv;
    private TextView stopTimeTv;
    private TextView statusTv;
    private TextView briefTv;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private SimpleDateFormat logFileDateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
    private StringBuilder runTimeSb = new StringBuilder();

    @Nullable
    private StressTestTask stressTestTask;
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
        void onConfigBeforeStart(StressTestTask testTask);

        /**
         * 在该回调中更新界面。主要更新测试状态。
         */
        void onUpdateStatus(StressTestTask testTask);
    }

    public StressTestHolder(CB callback) {
        this.callback = callback;
    }

    public StressTestTask getTest() {
        return stressTestTask;
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
        if (stressTestTask == null) {
            return;
        }
        if (stressTestTask.isStarted()) {
            return;
        }
        if (startBtn != null) {
            startBtn.setEnabled(ready);
        }
    }

    @NonNull
    public View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.libuihelper_fragment_stress_test, container, false);

        configLL = root.findViewById(R.id.libuihelper_fragment_stress_test_config_ll);
        statusLL = root.findViewById(R.id.libuihelper_fragment_stress_test_status_ll);

        totalCountHolder = new ValueEditorHolder()
                .attachView(root.findViewById(R.id.libuihelper_fragment_stress_test_count_ed))
                .enableNumInput()
                .setValue(1);
        maxFailHolder = new ValueEditorHolder()
                .attachView(root.findViewById(R.id.libuihelper_fragment_stress_test_max_fail_ed))
                .enableNumInput()
                .setValue(0);
        maxTimeHolder = new ValueEditorHolder()
                .attachView(root.findViewById(R.id.libuihelper_fragment_stress_test_max_time_ed))
                .enableNumInput()
                .setValue(0);
        maxHardFailHolder = new ValueEditorHolder()
                .attachView(root.findViewById(R.id.libuihelper_fragment_stress_test_max_hard_fail_ed))
                .enableNumInput()
                .setValue(0);

        statusLogCb = root.findViewById(R.id.libuihelper_fragment_stress_test_status_log_cb);
        saveLogCb = root.findViewById(R.id.libuihelper_fragment_stress_test_save_log_cb);
        alarmCb = root.findViewById(R.id.libuihelper_fragment_stress_test_alarm_cb);
        startBtn = root.findViewById(R.id.libuihelper_fragment_stress_test_start_btn);
        startBtn.setOnClickListener(new DebouncedClickListener(this));
        alarmCb.setOnClickListener(this);

        startTimeTv = root.findViewById(R.id.libuihelper_fragment_stress_test_start_time_tv);
        runTimeTv = root.findViewById(R.id.libuihelper_fragment_stress_test_run_time_tv);
        runCountTv = root.findViewById(R.id.libuihelper_fragment_stress_test_run_count_tv);
        failCountTv = root.findViewById(R.id.libuihelper_fragment_stress_test_fail_count_tv);
        stopTimeTv = root.findViewById(R.id.libuihelper_fragment_stress_test_stop_time_tv);
        statusTv = root.findViewById(R.id.libuihelper_fragment_stress_test_status_tv);
        briefTv = root.findViewById(R.id.libuihelper_fragment_stress_test_brief_tv);

        if (testLogger == null) {
            testLogger = new RingLogger(100);
            testLogger.setLogger(Log.getLogger());
        }
        logHelper.attachView(root.findViewById(R.id.libuihelper_fragment_stress_test_log_rv))
                .setRingLogger(testLogger);

        return root;
    }

    public void setTest(StressTestTask testTask) {
        stressTestTask = testTask;
        if (stressTestTask == null) {
            throw new NullPointerException("Test task is null.");
        } else {
            // 如果测试已经运行，就恢复显示
            if (stressTestTask.isStarted()) {
                testLogger.v(getClass().getSimpleName(), "恢复显示测试过程");
                startTest();
            }
        }
        //if (stressTestTask.getLogger() == null) {
        //    不需要在状态日志中显示Task的执行详情
        //}
        // 尝试自动获取BRIEF字段
        if (this.briefTv != null && this.briefTv.getVisibility() != View.VISIBLE) {
            try {
                Field brief = testTask.getClass().getDeclaredField("BRIEF");
                setBrief((String) brief.get(null));
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }
    }

    public void setBrief(String brief) {
        if (this.briefTv != null) {
            this.briefTv.setText(brief);
            this.briefTv.setVisibility(brief != null ? View.VISIBLE : View.GONE);
        }
    }

    public void destroy() {
        disposer.disposeAll(null);
    }

    @Override
    public void onClick(View v) {
        if (v == startBtn) {
            if (ready) {
                if (stressTestTask != null) {
                    if (stressTestTask.isStarted()) {
                        if (alarmCb.isChecked()) {
                            AlertDialog.Builder dlg = new AlertDialog.Builder(v.getContext());
                            dlg.setTitle("确认停止？");
                            dlg.setPositiveButton(android.R.string.ok, (dialog, which) -> stressTestTask.stopTest());
                            dlg.setNegativeButton(android.R.string.cancel, null);
                            dlg.show();
                        } else {
                            stressTestTask.stopTest();
                        }
                    } else {
                        startTest();
                    }
                } else {
                    testLogger.e(getClass().getSimpleName(), "Can't start test. Test is null.");
                }
            } else {
                testLogger.i(getClass().getSimpleName(), "Not ready to start test.");
            }
        } else if (v == alarmCb) {
            if (alarmCb.isChecked()) {
                Context ctx = alarmCb.getContext();
                AudioManager abc = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
                if (abc != null && abc.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
                    Toast.makeText(ctx, "设备已静音，请开启铃声。", Toast.LENGTH_LONG).show();
                    alarmCb.setChecked(false);
                }
            }
        }
    }

    private void startTest() {
        if (stressTestTask == null) return;

        stressTestTask.evtStart().setDisposer(disposer).setExecutor(UiExecutor.getDefault()).register2(((src, evtType, evtData) -> {
            enableUI(false);
            startTimeTv.setText(dateFormat.format(new Date()));
        }));

        stressTestTask.evtStatus().setDisposer(disposer).setExecutor(UiExecutor.getDefault()).register2(((src, evtType, evtData) -> {
            statusTv.setText(evtData);
            if (statusLogCb.isChecked()) {
                testLogger.i("status", evtData);
            }
        }));

        stressTestTask.evtTestError().setDisposer(disposer).setExecutor(UiExecutor.getDefault()).register2(((src, evtType, evtData) -> {
            testLogger.e("status", evtData.failNumber + "-" + evtData.testNumber + ": " + evtData.msg);
            if (saveLogCb.isChecked()) {
                try {
                    final File dir = new File(saveLogCb.getContext().getExternalFilesDir(null), "test");
                    final File testCaseDir = new File(dir, logFileDateFormat.format(new Date(stressTestTask.getStartTimestamp())));
                    testLogger.saveTo(new File(testCaseDir, evtData.failNumber + "-" + evtData.testNumber + "-" + evtData.timestamp + ".log"));
                    Log.getLogger().saveTo(new File(testCaseDir, evtData.failNumber + "-" + evtData.testNumber + "-" + evtData.timestamp + "-sys.log"));
                    LogcatUtil.saveAndroidLog(new File(testCaseDir, evtData.failNumber + "-" + evtData.testNumber + "-" + evtData.timestamp + "-logcat.log"));

                    File reportFile = new File(testCaseDir, "_report_.csv");
                    boolean reportFileExists = reportFile.exists();
                    FileWriter writer = new FileWriter(reportFile, true);
                    // write header
                    if (!reportFileExists) {
                        writer.write("Time,Fail#,Test#,Timestamp,Message\n");
                    }
                    // write record
                    String sb = dateFormat.format(new Date(evtData.timestamp)) + "," +
                            evtData.failNumber + "," +
                            evtData.testNumber + "," +
                            evtData.timestamp + "," +
                            "\"" + evtData.msg + "\"\n";
                    writer.write(sb);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));

        stressTestTask.evtFinished().setDisposer(disposer).setExecutor(UiExecutor.getDefault()).register2(((src, evtType, evtData) -> {
            // save test info
            final File dir = new File(saveLogCb.getContext().getExternalFilesDir(null), "test");
            final File testCaseDir = new File(dir, logFileDateFormat.format(new Date(stressTestTask.getStartTimestamp())));
            try {
                if (testCaseDir.exists()) {
                    FileWriter writer = new FileWriter(new File(testCaseDir, "_report_.csv"), true);
                    // write record
                    String sb = "Start," + dateFormat.format(new Date(stressTestTask.getStartTimestamp())) + "\n" +
                            "Stop," + dateFormat.format(new Date(stressTestTask.getStopTimestamp())) + "\n" +
                            "Elapse," + ((stressTestTask.getStopTimestamp() - stressTestTask.getStartTimestamp()) / 1000) + "\n" +
                            "TestCount," + stressTestTask.getTestCount() + "/" + stressTestTask.getTotalCount() + "\n" +
                            "FailCount," + stressTestTask.getFailCount() + "/" + stressTestTask.getMaxFail() + "\n";
                    writer.write(sb);
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (evtData.getError() != null) {
                statusTv.setText(evtData.getError().getMessage());
                String message = evtData.getError().getRootCause().getMessage();
                testLogger.e("status", message != null ? message : evtData.getError().getRootCause().toString());
            } else {
                statusTv.setText("测试完成");
                testLogger.i("status", "测试完成");
            }
            enableUI(true);
            disposer.disposeAll(null);
            updateRunTimeTv((stressTestTask.getStopTimestamp() - stressTestTask.getStartTimestamp()) / 1000);
            stopTimeTv.setText(dateFormat.format(new Date(stressTestTask.getStopTimestamp())));
            if (callback != null) {
                callback.onUpdateStatus(stressTestTask);
            }

            if (!(evtData.getError() instanceof TaskError.Abort)) {
                alertCompletion();
            }
        }));

        stopTimeTv.setText(R.string.libuihelper_na);

        if (stressTestTask.isStarted()) {
            enableUI(false);
            startTimeTv.setText(dateFormat.format(new Date(stressTestTask.getStartTimestamp())));

        } else {
            testLogger.clearSync();
            // 配置测试任务
            if (callback != null) {
                callback.onConfigBeforeStart(stressTestTask);
            }
            stressTestTask.setConfig(totalCountHolder.getValue(), maxFailHolder.getValue(), maxTimeHolder.getValue() * 1000);
            stressTestTask.setMaxHardFail(maxHardFailHolder.getValue());
            stressTestTask.setExecutor(UiExecutor.getDefault());
            //不需要在状态日志中显示Task的执行详情
            stressTestTask.start(null, null);
        }

        updateTimer.post(this);
    }

    private void alertCompletion() {
        if (alarmCb.isChecked()) {
            Context ctx = alarmCb.getContext();
            AlertDialog.Builder dlg = new AlertDialog.Builder(ctx);
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            Ringtone ringtone = RingtoneManager.getRingtone(ctx, uri);
            if (ringtone != null) {
                ringtone.play();
                dlg.setTitle("测试结束！");
                dlg.setPositiveButton(android.R.string.ok, null);
                dlg.setOnDismissListener(dialog -> ringtone.stop());
                dlg.show();
            }
        }
    }

    @Override
    public void run() {
        if (stressTestTask != null) {
            runCountTv.setText((stressTestTask.getTestCount()) + "次");
            failCountTv.setText((stressTestTask.getFailCount()) + "次");

            if (stressTestTask.isStarted()) {
                updateRunTimeTv((System.currentTimeMillis() - stressTestTask.getStartTimestamp()) / 1000);
                updateTimer.removeCallbacks(this);
                updateTimer.postDelayed(this, 1000);
            }

            if (callback != null) {
                callback.onUpdateStatus(stressTestTask);
            }
        }
    }

    private void enableUI(boolean enable) {
        //containerLL.setEnabled(enable);
        enableViewGroup(configLL, enable);
        totalCountHolder.setEnabled(enable);
        maxFailHolder.setEnabled(enable);
        maxTimeHolder.setEnabled(enable);
        maxHardFailHolder.setEnabled(enable);
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

    private void updateRunTimeTv(long second) {
        runTimeSb.delete(0, runTimeSb.length());
        int sec = (int) second;
        if (sec >= 3600) {
            runTimeSb.append(sec / 3600).append("时");
            sec %= 3600;
        }
        if (sec >= 60) {
            runTimeSb.append(sec / 60).append("分");
            sec %= 60;
        }
        runTimeSb.append(sec).append("秒");
        runTimeTv.setText(runTimeSb);
    }
}
