package com.goodix.ble.libcomx.task;

public class SimpleTask extends Task {
    @TaskParameter
    Integer intParam;
    @TaskParameter
    Float floatParamNull;

    @Override
    protected int doWork() {
        System.out.println("Task is running on thread: " + Thread.currentThread().getName());
        System.out.println("Task intParam: " + intParam);
        finished(ITaskResult.CODE_DONE, null);
        return 0;
    }
}
