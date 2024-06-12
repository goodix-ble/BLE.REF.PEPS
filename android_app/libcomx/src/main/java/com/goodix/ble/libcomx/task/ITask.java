package com.goodix.ble.libcomx.task;

import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.Event;

import java.util.concurrent.Executor;

public interface ITask extends Runnable, ITaskContext {

    int EVT_START = 340;
    int EVT_PROGRESS = 341;
    int EVT_FINISH = 342;

    String getName();

    /**
     * 开始执行任务
     *
     * @param ctx      当前task运行的上下文。
     * @param prevTask 前一个运行的任务，用于获取前一个任务执行的结果，实现结果的传递。可以为 null 。
     */
    void start(ITaskContext ctx, ITask prevTask);

    /**
     * Stop the task.
     */
    void abort();

    Event<Void> evtStart();

    Event<ITaskResult> evtFinished();

    Event<Integer> evtProgress();

    ITask setName(String name);

    ITask setLogger(ILogger logger);

    ITask setExecutor(Executor executor);

    ITask setDebug(boolean enable);

    ITask setDebounceProgressEvent(boolean debounce);

    ITask clearListener(Object tag);

    ILogger getLogger();

    boolean isStarted();

    boolean isFinished();

    int getProgress();

    /**
     * 获取任务输出的结果，或者任务的本地参数。主要用于输出结果
     *
     * @return 当key为null时，返回一个包含所有key的List。
     * 没有找到key对应的value时，返回null。
     */
    <T> T getOutput(String key);

    ITaskResult getResult();

    <T> T getPreviousTask();
}
