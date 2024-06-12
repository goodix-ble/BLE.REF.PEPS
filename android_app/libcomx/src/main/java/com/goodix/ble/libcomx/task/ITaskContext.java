package com.goodix.ble.libcomx.task;

import java.util.concurrent.Executor;

public interface ITaskContext {

    <T> T getParameter(String key);

    <T> void setParameter(String key, T val);

    Executor getExecutor();
}
