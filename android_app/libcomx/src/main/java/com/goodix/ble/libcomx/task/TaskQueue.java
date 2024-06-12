package com.goodix.ble.libcomx.task;

import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.event.IEventListener;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnusedReturnValue")
public class TaskQueue extends Task implements ITaskSet {

    @SuppressWarnings("WeakerAccess")
    protected boolean abortOnException = false;
    protected boolean useResultOfSubtask = false;
    @SuppressWarnings("WeakerAccess")
    protected boolean requestAbortQueue = false;

    private int currentTaskPos;
    private List<ITask> mTaskContextList = new ArrayList<>();

    private int[] mTaskPercentWeights = null;

    private ITask currentTask;

    private Event<ITask> eventSubtaskStart = new Event<>(this, ITaskSet.EVT_SUBTASK_START);
    private Event<ITask> eventSubtaskProgress = new Event<>(this, ITaskSet.EVT_SUBTASK_PROGRESS);
    private Event<ITask> eventSubtaskFinish = new Event<>(this, ITaskSet.EVT_SUBTASK_FINISH);
    private SubTaskListener subTaskEventHandler = new SubTaskListener();

    private int queueTimeout = 10 * 60 * 1000; // 10 minutes as default.

    public synchronized TaskQueue addTask(ITask task) {
        if (isStarted()) throw new IllegalStateException("Task is already started.");

        mTaskContextList.add(task);

        return this;
    }

    public <T extends ITask> T addTask2(T task) {
        addTask(task);
        return task;
    }

    public synchronized TaskQueue clearTask() {
        if (isStarted()) throw new IllegalStateException("Task is already started.");

        for (ITask task : mTaskContextList) {
            task.evtFinished().remove(subTaskEventHandler);
            task.evtProgress().remove(subTaskEventHandler);
            task.evtStart().remove(subTaskEventHandler);
        }
        mTaskContextList.clear();

        return this;
    }

    /**
     * 获取指定索引号的子任务。
     *
     * @param idx 任务索引号，0表示第一个任务。支持负数索引，-1表示倒数第一个任务。
     * @return 子任务。没有找到时，返回null。
     */
    public synchronized ITask getTask(int idx) {
        final int taskCnt = mTaskContextList.size();

        if (idx < 0) {
            idx = taskCnt + idx;
            if (idx < 0) idx = 0; // 确保为正整数
        }

        if (idx < taskCnt) {
            return mTaskContextList.get(idx);
        } else {
            return null;
        }
    }

    /**
     * 替换一个任务。
     *
     * @param idx     即将被替换的任务索引号，0表示第一个任务。支持负数索引，-1表示倒数第一个任务。
     * @param newTask 新的任务
     * @return 被替换掉的任务。如果索引号错误，则返回null
     */
    public synchronized ITask replaceTask(int idx, ITask newTask) {
        final int taskCnt = mTaskContextList.size();

        if (idx < 0) {
            idx = taskCnt + idx;
            if (idx < 0) idx = 0; // 确保为正整数
        }

        if (idx < taskCnt) {
            ITask old = mTaskContextList.get(idx);
            mTaskContextList.set(idx, newTask);
            return old;
        } else {
            return null;
        }
    }

    public synchronized int getTaskCount() {
        return mTaskContextList.size();
    }

    public TaskQueue setTaskPercentWeights(int... weights) {
        if (weights != null && weights.length > 0) {
            // 校验每个任务的百分比，并校验总和
            int sum = 0;
            for (int i = 0; i < weights.length; i++) {
                int weight = weights[i];
                if (weight > 0) {
                    sum += weight;
                } else {
                    throw new IllegalArgumentException("Weight of subtask must > 0 at [" + i + "]: " + weight);
                }
            }

            if (sum != 100) {
                throw new IllegalArgumentException("Expected total weight is 100, but actual weight is: " + sum);
            }

            mTaskPercentWeights = weights;
        } else {
            mTaskPercentWeights = null;
        }
        return this;
    }

    public TaskQueue setAbortOnException() {
        abortOnException = true;
        return this;
    }

    public TaskQueue setAbortOnException(boolean useResultOfSubtask) {
        this.abortOnException = true;
        this.useResultOfSubtask = useResultOfSubtask;
        return this;
    }

    public TaskQueue setQueueTimeout(int ms) {
        this.queueTimeout = ms;
        return this;
    }

    @Override
    public Event<ITask> evtSubtaskStart() {
        return eventSubtaskStart;
    }

    @Override
    public Event<ITask> evtSubtaskProgress() {
        return eventSubtaskProgress;
    }

    @Override
    public Event<ITask> evtSubtaskFinish() {
        return eventSubtaskFinish;
    }

    @Override
    public ITask setLogger(ILogger logger) {
        super.setLogger(logger);
        synchronized (this) {
            for (ITask task : mTaskContextList) {
                task.setLogger(logger);
            }
        }
        return this;
    }

    @Override
    public ITask setDebug(boolean enable) {
        synchronized (this) {
            for (ITask task : mTaskContextList) {
                task.setDebug(enable);
            }
        }
        return super.setDebug(enable);
    }

    @Override
    public ITask clearListener(Object tag) {
        evtSubtaskStart().clear(tag);
        evtSubtaskProgress().clear(tag);
        evtSubtaskFinish().clear(tag);
        return super.clearListener(tag);
    }

    @Override
    protected void onStart() {
        currentTask = null;
        currentTaskPos = -1; // 刚开始时，没有执行任何任务
        requestAbortQueue = false;
    }

    @Override
    protected void onCleanup() {
        // 结束当前的子任务
        final ITask curTask = this.currentTask;
        if (curTask != null) {
            curTask.abort();
        }
        requestAbortQueue = true;
    }

    @Override
    public int doWork() {
        // 百分比必须和任务数相等
        if (mTaskPercentWeights != null && mTaskPercentWeights.length != mTaskContextList.size()) {
            finishedWithError("Must set weight for each subtask.");
            return 0;
        }

        scheduleTask();

        return queueTimeout;
    }

    private synchronized void scheduleTask() {
        if (taskState != STATE_RUNNING) {
            return;
        }

        final ILogger log = this.logger;
        int taskCnt = mTaskContextList.size();
        int taskIdx = ++currentTaskPos; // 移动到下一个任务
        if (taskIdx < taskCnt) {
            // 将当前执行的任务保存为前置任务
            ITask prevTaskForNextTask = currentTask;
            if (prevTaskForNextTask == null) {
                prevTaskForNextTask = this.prevTask;
            }

            // 获取下一个任务为当前任务
            currentTask = mTaskContextList.get(taskIdx);

            // 配置任务并启动
            try {
                if (log != null && printVerboseLog) {
                    log.v(getName(), "Start subtask #" + taskIdx + ": " + currentTask.getName());
                }

                // 任务开始前添加监听
                currentTask.evtFinished().register(subTaskEventHandler);
                currentTask.evtProgress().register(subTaskEventHandler);
                currentTask.evtStart().register(subTaskEventHandler);

                // 开始任务
                currentTask.start(this, prevTaskForNextTask);
            } catch (Exception e) {
                // 无法启动子任务
                e.printStackTrace();
                final String msg = "Exception on starting subtask #" + currentTaskPos + ", " + currentTask.getName() + ": " + e.getMessage();
                // 判断是否继续
                if (abortOnException) {
                    finished(ITaskResult.CODE_ERROR, new TaskError(this, msg, e));
                } else {
                    if (log != null) {
                        log.e(getName(), msg);
                    }
                    tryStartNextTask(); // 遇到异常时，跳过并执行下一个子任务
                }
            }
        } else {
            finishedWithDone();
        }
    }

    // 综合计算子任务的进度
    private void publishWithChildProgress(int percent) {
        int basePercent = 0;
        int biasPercent;

        // 保护计算过程
        synchronized (this) {
            int taskIdx = currentTaskPos;
            int taskCnt = mTaskContextList.size();
            final int[] weights = this.mTaskPercentWeights;

            if (weights != null) {
                for (int i = 0; i < taskIdx; i++) {
                    basePercent += weights[i];
                }
                biasPercent = weights[taskIdx];
            } else {
                basePercent = 100 * taskIdx / taskCnt;
                biasPercent = 100 / taskCnt;
            }
        }

        // 限制范围
        if (percent > 100) percent = 100;
        if (percent < 0) percent = 0;

        publishProgress(basePercent + (biasPercent * percent / 100));
    }

    private void tryStartNextTask() {
        ITask curTask = null;
        int taskPos = -1;
        boolean running;

        synchronized (this) {
            running = (taskState == STATE_RUNNING);
            if (abortOnException) {
                // 确保这两个参数是同时被获取的
                curTask = this.currentTask;
                taskPos = this.currentTaskPos;
            }
        }
        if (running) {
            // 判断当前任务是否有异常
            if (curTask != null) {
                ITaskResult result = curTask.getResult();
                if (result.getError() != null) {
                    if (useResultOfSubtask) {
                        finished(result.getCode(), result.getError());
                    } else {
                        finished(ITaskResult.CODE_ERROR, new TaskError(this, "Abort at subtask #" + taskPos + ": " + curTask.getName(), result.getError()));
                    }
                    return;
                }
            }

            getExecutor().execute(this::scheduleTask);
        }
    }

    class SubTaskListener implements IEventListener {
        /**
         * 从子任务的线程中调用的
         */
        @Override
        public void onEvent(Object src, int evtType, Object evtData) {
            final ILogger log = TaskQueue.this.logger;
            ITask subTask = (ITask) src;
            ITask curTask;
            int curTaskPos;

            synchronized (TaskQueue.this) {
                curTask = currentTask;
                curTaskPos = currentTaskPos;
            }

            // 非当前任务不处理
            if (subTask == curTask) {
                if (evtType == ITask.EVT_START) {
                    // 刷新一下任务队列的进度，表示一个子任务开始了
                    publishWithChildProgress(0); // percent写0的时候表示某个任务启动时对应的起点percent
                    eventSubtaskStart.postEvent(subTask);

                } else if (evtType == ITask.EVT_PROGRESS) {
                    publishWithChildProgress((Integer) evtData);
                    eventSubtaskProgress.postEvent(subTask);

                } else if (evtType == ITask.EVT_FINISH) {
                    ITaskResult result = (ITaskResult) evtData;

                    if (log != null && printVerboseLog) {
                        if (result.getError() != null) {
                            log.v(getName(), "Subtask #" + curTaskPos + ", " + subTask.getName()
                                    + ", is finished with error: " + result.getError().getMessage()
                                    + ". Start next one.");
                        } else {
                            log.v(getName(), "Subtask #" + curTaskPos + ", " + subTask.getName() + ", is finished. Start next one.");
                        }
                    }

                    // 任务完成后移除监听
                    subTask.evtStart().remove(subTaskEventHandler);
                    subTask.evtProgress().remove(subTaskEventHandler);
                    subTask.evtFinished().remove(subTaskEventHandler);

                    eventSubtaskFinish.postEvent(subTask);

                    tryStartNextTask();
                }
            } else {
                if (log != null) {
                    log.w(getName(), "Unexpected event: src = " + subTask.getName()
                            + ", type = " + evtType
                            + ", data = " + evtData);
                }
            }
        }
    }
}
