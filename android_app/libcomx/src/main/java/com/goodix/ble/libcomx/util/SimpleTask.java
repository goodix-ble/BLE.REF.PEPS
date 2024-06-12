package com.goodix.ble.libcomx.util;

import com.goodix.ble.libcomx.task.ITaskResult;
import com.goodix.ble.libcomx.task.Task;
import com.goodix.ble.libcomx.task.TaskError;

public class SimpleTask extends Task {
    private Object tag;
    private int taskId;
    private Work work;

    public interface Work {
        void onWork(SimpleTask host, Object taskTag) throws Throwable;
    }

    public SimpleTask() {
    }

    public SimpleTask(Work work) {
        this.work = work;
    }

    public SimpleTask(String name, Object taskTagOrId, Work work) {
        this.work = work;
        this.tag = taskTagOrId;
        setName(name);
        if (taskTagOrId instanceof Integer) {
            this.taskId = (int) taskTagOrId;
        }
    }

    public SimpleTask setTag(Object tag) {
        this.tag = tag;
        return this;
    }

    public SimpleTask setTaskId(int taskId) {
        this.taskId = taskId;
        return this;
    }

    public int getTaskId() {
        return taskId;
    }

    public SimpleTask setWork(Work work) {
        this.work = work;
        return this;
    }

    @Override
    protected int doWork() {
        if (work != null) {
            try {
                work.onWork(this, tag);
            } catch (Throwable e) {
                TaskError err;
                if (e instanceof TaskError) {
                    err = (TaskError) e;
                } else {
                    err = new TaskError(this, e.getMessage(), e);
                }
                finished(ITaskResult.CODE_ERROR, err);
            }
        }
        return 0;
    }

}
