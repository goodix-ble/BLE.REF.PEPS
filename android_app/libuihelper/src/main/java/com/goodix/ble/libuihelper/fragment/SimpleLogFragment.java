package com.goodix.ble.libuihelper.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.goodix.ble.libcomx.logger.RingLogger;
import com.goodix.ble.libuihelper.R;
import com.goodix.ble.libuihelper.input.DebouncedClickListener;
import com.goodix.ble.libuihelper.input.EditTextCleanupHelper;
import com.goodix.ble.libuihelper.logger.Log;
import com.goodix.ble.libuihelper.logger.LogRecyclerViewHelper;

import java.io.File;


public class SimpleLogFragment extends ClosableTabFragment implements TabMgrFragment.ITabItem {

    private LogRecyclerViewHelper logRecyclerViewHelper;

    @Nullable
    private RingLogger ringLogger;

    public SimpleLogFragment setRingLogger(RingLogger ringLogger) {
        this.ringLogger = ringLogger;
        if (ringLogger != null && logRecyclerViewHelper != null) {
            logRecyclerViewHelper.setRingLogger(ringLogger);
        }
        return this;
    }

    @Nullable
    public RingLogger getRingLogger() {
        return ringLogger;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (tabTitle == null) {
            final String tag = getTag();
            if (tag != null) {
                setTabTitle(tag);
            } else {
                setTabTitle("LOG");
            }
        }

        View root = inflater.inflate(R.layout.libuihelper_fragment_simple_log, container, false);

        ViewGroup logLayout = root.findViewById(R.id.libuihelper_fragment_simple_log_ll);
        logLayout.setVisibility(View.VISIBLE);

        logRecyclerViewHelper = new LogRecyclerViewHelper()
                .attachView(logLayout,
                        R.id.libuihelper_fragment_simple_log_rv,
                        R.id.libuihelper_fragment_simple_log_level_spinner,
                        0, // save button 单独处理，加上提示
                        R.id.libuihelper_fragment_simple_log_clear_btn,
                        R.id.libuihelper_fragment_simple_log_bottom_btn,
                        R.id.libuihelper_fragment_simple_log_search_ed);
        logLayout.findViewById(R.id.libuihelper_fragment_simple_log_save_btn).setOnClickListener(new DebouncedClickListener(v -> {
            if (ringLogger == null) {
                return;
            }

            Context ctx = v.getContext();

            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setMessage("Are you sure to save " + ringLogger.size() + " log(s) ?")
                    .setPositiveButton(android.R.string.ok, ((dialog, which) -> {
                        File logFile = logRecyclerViewHelper.saveLog(ctx);
                        AlertDialog.Builder resultDlg = new AlertDialog.Builder(ctx);
                        if (logFile != null) {
                            resultDlg.setTitle("Logs are stored")
                                    .setMessage(logFile.getAbsolutePath())
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show();
                        } else {
                            resultDlg.setMessage("Failed to save logs.")
                                    .show();
                        }
                    }))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();

        }));

        final EditText searchEd = logLayout.findViewById(R.id.libuihelper_fragment_simple_log_search_ed);
        new EditTextCleanupHelper().attach(searchEd).evtClear().register(((src, evtType, evtData) -> logRecyclerViewHelper.setLogFilter(null)));

        // 如果没有指定日志记录器，就采用默认的日志记录器
        if (ringLogger == null) {
            ringLogger = Log.getLogger();
        }
        logRecyclerViewHelper.setRingLogger(ringLogger);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        logRecyclerViewHelper.destroy();
    }

    @Override
    public void onSelectionChanged(boolean selected) {
        if (logRecyclerViewHelper != null) {
            logRecyclerViewHelper.setPauseUpdate(!selected);
        }
    }
}
