package com.goodix.ble.libcomx.task;

@SuppressWarnings("ALL")
public class TaskError extends Error {
    private ITask task;
    private String extMessage; // 避免反复拼接消息

    public TaskError(ITask task) {
        this.task = task;
    }

    public TaskError(ITask task, String s) {
        super(s);
        this.task = task;
    }

    public TaskError(ITask task, String s, Throwable throwable) {
        super(s, throwable);
        this.task = task;
    }

    public TaskError(ITask task, Throwable throwable) {
        super(throwable);
        this.task = task;
    }

    public ITask getTask() {
        return task;
    }

    public Throwable getRootCause() {
        int antiInfiniteLoop = 0xFFFF;
        Throwable rootCause = this;
        while (rootCause.getCause() != null && antiInfiniteLoop != 0) {
            rootCause = rootCause.getCause();
            antiInfiniteLoop--;
        }
        return rootCause;
    }

    public static class Abort extends TaskError {
        public Abort(ITask task) {
            super(task, "Abort");
        }

        public Abort(ITask task, String msg) {
            super(task, msg);
        }
    }

    public static class Timeout extends TaskError {
        public Timeout(ITask task) {
            super(task, "Timeout");
        }

        public Timeout(ITask task, String msg) {
            super(task, msg);
        }
    }

    /**
     * 方便打印异常调用栈时，能把Task的名称自动带出来
     */
    @Override
    public String getMessage() {
        if (extMessage == null) {
            extMessage = "[" + task.getName() + "]: " + super.getMessage();
        }
        return extMessage;
    }

    public String getRawMessage() {
        return super.getMessage();
    }
}
