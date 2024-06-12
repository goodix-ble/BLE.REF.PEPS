package com.goodix.ble.libcomx.task;

public interface ITaskResult {
    int CODE_DONE = 0;
    int CODE_ERROR = -1;
    int CODE_ABORT = -2;
    int CODE_TIMEOUT = -3;
    int CODE_SKIP = -4;

    ITask getTask();

    int getCode();

    TaskError getError();
}
