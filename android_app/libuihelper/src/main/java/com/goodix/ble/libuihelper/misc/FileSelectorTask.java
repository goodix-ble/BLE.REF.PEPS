package com.goodix.ble.libuihelper.misc;

import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libcomx.task.ITaskResult;
import com.goodix.ble.libcomx.task.Task;
import com.goodix.ble.libcomx.task.TaskOutput;
import com.goodix.ble.libcomx.task.TaskParameter;
import com.goodix.ble.libuihelper.sublayout.FileSelectorHolder;
import com.goodix.ble.libuihelper.thread.UiExecutor;

public class FileSelectorTask extends Task implements IEventListener<Boolean> {

    @TaskParameter
    @TaskOutput
    private FileSelectorHolder fileSelector;

    private int timeout = 120_000;
    private boolean errorOnCancel = false;


    public FileSelectorTask setFileSelector(FileSelectorHolder fileSelector) {
        this.fileSelector = fileSelector;
        return this;
    }

    public FileSelectorTask setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public FileSelectorTask setErrorOnCancel(boolean errorOnCancel) {
        this.errorOnCancel = errorOnCancel;
        return this;
    }

    @Override
    protected int doWork() {
        fileSelector.evtSelected()
                .subEvent(this).setExecutor(UiExecutor.getDefault())
                .register2(this);

        if (fileSelector.show()) {
            return timeout;
        } else {
            finishedWithError("Failed to show the file selector.");
        }

        return 0;
    }

    @Override
    protected void onCleanup() {
        if (fileSelector != null) {
            fileSelector.evtSelected().clear(this);
        }
    }

    @Override
    public void onEvent(Object src, int evtType, Boolean selected) {
        if (!selected) {
            fileSelector.clearSelection();
        }
        if (errorOnCancel && !selected) {
            finishedWithError(ITaskResult.CODE_ABORT, "Cancelled");
        } else {
            setParameter(FileSelectorHolder.class, fileSelector);
            finishedWithDone();
        }
    }
}
