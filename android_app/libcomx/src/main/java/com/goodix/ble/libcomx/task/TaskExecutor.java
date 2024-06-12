package com.goodix.ble.libcomx.task;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 工具类：
 * 如果任务没有指定执行线程，那么就会使用一个默认的线程池来执行，
 * 从而避免创建太多的线程池。
 * <p>
 * 立即执行器应用于需要消除调度延时的高时效场景。请谨慎使用。
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class TaskExecutor {
    private static Executor DEFAULT_EXECUTOR = null;
    private static Executor IMMEDIATE_EXECUTOR = null;

    public static Executor getImmediateExecutor() {
        if (IMMEDIATE_EXECUTOR == null) {
            synchronized (TaskExecutor.class) {
                if (IMMEDIATE_EXECUTOR == null) {
                    IMMEDIATE_EXECUTOR = Runnable::run;
                }
            }
        }
        return IMMEDIATE_EXECUTOR;
    }


    public static Executor getDefaultExecutor() {
        if (DEFAULT_EXECUTOR == null) {
            synchronized (TaskExecutor.class) {
                if (DEFAULT_EXECUTOR == null) {
                    DEFAULT_EXECUTOR = Executors.newScheduledThreadPool(1);
                }
            }
        }
        return DEFAULT_EXECUTOR;
    }

    public static void setDefaultExecutor(Executor executor) {
        if (executor == null) {
            return;
        }

        ExecutorService preExecutor = null;

        synchronized (TaskExecutor.class) {
            if (executor != DEFAULT_EXECUTOR) {
                preExecutor = DEFAULT_EXECUTOR instanceof ExecutorService ? ((ExecutorService) DEFAULT_EXECUTOR) : null;
                DEFAULT_EXECUTOR = executor;
            }
        }

        if (preExecutor != null && !preExecutor.isShutdown()) {
            try {
                preExecutor.shutdown();
            } catch (Exception ignored) {
            }
        }
    }

    private TaskExecutor() {
    }
}
