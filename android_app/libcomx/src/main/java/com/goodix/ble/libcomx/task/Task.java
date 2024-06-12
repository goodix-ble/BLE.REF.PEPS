package com.goodix.ble.libcomx.task;


import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.annotation.Nullable;
import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.util.CallUtil;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class Task implements ITask, ITaskResult {
    private static final int TIMER_ID_TASK_TIME_OUT = -277;

    private String taskName = getClass().getSimpleName();
    private Executor mExecutor;

    @TaskParameter(nullable = true)
    protected ILogger logger = debugLogger;

    protected boolean isAborted = false; // 终止信号
    private boolean isFinished = false; // 用于判断任务是否执行过，并且结束了
    private boolean hasRun = false; // 保证run()只被执行一次
    //private boolean didWork = false; // 标记doWork是否已经执行。解决问题：finish()调用在doWork()前面，导致onCleanup()因为在doWork()前调用而无法有效清除回调的问题。
    private final boolean reusableTask; // 如果要重复使用，需要处理好外部回调及时清理的问题。设置为false时表示该任务为一次性任务。
    private boolean debounceProgressEvent = false; // 防止因为进度缓慢，很久都不产生进度事件，导致监听进度事件的其它任务超时。其实进度是在持续增加，但因为不计算小数点，所以容易被忽略。

    protected int taskState = STATE_IDLE;
    protected static final int STATE_IDLE = 0; // 仅finish()和类构造可以设置
    protected static final int STATE_STARTING = 1; // 仅start()可以设置
    protected static final int STATE_RUNNING = 2; // 仅start()可以设置。RUNNING分为：doWork()执行中、doWork()执行后挂起等待中，通过taskPending标志来区分
    protected static final int STATE_STOPPING = 3; // 仅finish()可以设置
    protected static final int STATE_FINISHED = 4; // 仅finish()可以设置。reusableTask为false时才会进入该状态，且永远无法退出。

    private final Event<Void> eventStart = new Event<>(this, EVT_START);
    private final Event<Integer> eventProgress = new Event<>(this, EVT_PROGRESS);
    private final Event<ITaskResult> eventFinish = new Event<>(this, EVT_FINISH);

    private boolean clearListenerAfterFinished = false;
    protected boolean printVerboseLog = (debugLogger != null);
    public static ILogger debugLogger = null; // 为了调试Task


    @SuppressWarnings("WeakerAccess")
    protected ITaskContext rootCtx; // 记录父任务的上下文，优先使用它。
    @SuppressWarnings("FieldCanBeLocal")
    protected ITask prevTask;

    @Nullable
    private HashMap<String, Object> envParameter = null;
    private int taskResultCode;
    private TaskError taskResultError;

    @Nullable
    private Timer mTimer;
    private boolean taskPending = false; // 在run的时候设置为true，只在finish()中设置为false。确保挂起的，一定被cleanup了。
    private long timeoutInterval = 0;
    private long timeoutTimeStamp = 0;
    private int lastPublishedPercent;

    public Task() {
        this.reusableTask = true; // 任务通过在onCleanup()中及时解除回调注册，可以实现任务的重复利用。
    }

    public Task(String taskName) {
        this.taskName = taskName;
        this.reusableTask = true;
    }

    public Task(boolean reusableTask) {
        this.reusableTask = reusableTask;
    }

    public final Task setExecutor(Executor executor) {
        this.mExecutor = executor;
        return this;
    }

    @Override
    public ITask setDebug(boolean enable) {
        this.printVerboseLog = enable;
        return this;
    }

    @Override
    public ITask clearListener(Object tag) {
        eventStart.clear(tag);
        eventProgress.clear(tag);
        eventFinish.clear(tag);
        return this;
    }

    public final Task setParameter(Class type, Object param) {
        setParameter(type.getName(), param);
        return this;
    }

    public final Task setParameter(ITaskParameter param) {
        setParameter(param.getClass().getName(), param);
        return this;
    }

    /**
     * Remove all listeners which register to this task.
     * The default value is false.
     */
    public final Task setOneshot(boolean clearAfterFinish) {
        clearListenerAfterFinished = clearAfterFinish;
        return this;
    }

    @Override
    public final Task setDebounceProgressEvent(boolean debounceProgressEvent) {
        this.debounceProgressEvent = debounceProgressEvent;
        return this;
    }

    @Override
    public Task setName(String name) {
        this.taskName = name;
        return this;
    }

    @Override
    public String getName() {
        return taskName;
    }

    @Override
    public ITask setLogger(ILogger logger) {
        this.logger = logger;
        return this;
    }

    @Override
    public ILogger getLogger() {
        return this.logger;
    }

    @Override
    public final Event<Void> evtStart() {
        return eventStart;
    }

    @Override
    public final Event<ITaskResult> evtFinished() {
        return eventFinish;
    }

    @Override
    public final Event<Integer> evtProgress() {
        return eventProgress;
    }

    protected final synchronized void finished(int resultCode, TaskError e) {
        // finish()执行前，一定为非IDLE，执行后才为IDLE，所以，对start()，abort()是安全的
        // 因为锁定为了STATE_STOPPING状态，所以对finish()自身是安全的
        synchronized (this) {
            if (taskState != STATE_RUNNING) {
                if (logger != null) {
                    logger.w(getName(), "Task is not running. Do not call finished() again with: resultCode = [" + resultCode + "], e = [" + e + "], from " + CallUtil.trace(5));
                }
                return;
            }

            taskState = STATE_STOPPING;
            taskPending = false;
            isFinished = true;
            taskResultCode = resultCode;
            taskResultError = e;

            if (logger != null) {
                if (e == null) {
                    if (printVerboseLog) {
                        logger.v(getName(), "finished with: resultCode = [" + resultCode + "]");
                    }
                } else {
                    Throwable rootCause = e;
                    while (rootCause.getCause() != null) rootCause = rootCause.getCause();
                    logger.e(getName(), "finished with: resultCode = [" + resultCode + "], rootCause = [" + rootCause.getMessage() + "]"); // 不展开打印 error
                }
            }
        }

        try {
            stopTimer();
            onCleanup();
            eventFinish.postEvent(this);
        } catch (Exception err) {
            // 在结束的时候出现错误
            synchronized (this) {
                String errMsg = "Error is occurred while exiting: " + err.getMessage();
                taskResultCode = ITaskResult.CODE_ERROR;
                taskResultError = new TaskError(this, errMsg, err);
            }
            try {
                eventFinish.postEvent(this);
            } catch (Exception nop) {
                if (logger != null) {
                    logger.e(getName(), "Error is occurred in Finish Event: " + nop.getMessage(), nop);
                } else {
                    nop.printStackTrace();
                }
            }
        }

        // 这里可能会因为线程安全导致刚添加进去的listener被清除掉
        if (clearListenerAfterFinished || !reusableTask) {
            clearListener(null);
        }

        synchronized (this) {
            if (reusableTask) {
                taskState = STATE_IDLE; // 保障了 start() 的幂等性，没有完全结束前就不能再start()
            } else {
                taskState = STATE_FINISHED; // 不再重用任务
            }
        }
    }

    protected final void publishProgress(int percent) {
        boolean canSend = true;
        final ILogger log = this.logger;

        synchronized (this) {
            // 非运行态，不允许报进度
            if (taskState != STATE_RUNNING || isAborted) {
                return;
            }

            if (log != null && printVerboseLog) {
                log.v(getName(), "publishProgress: " + percent);
            }

            // refreshTaskTimeout();
            if (timeoutInterval > 0) {
                timeoutTimeStamp = System.currentTimeMillis() + timeoutInterval;
            }

            // 减少重复的percent上报
            if (debounceProgressEvent) {
                canSend = lastPublishedPercent != percent;
            }
            lastPublishedPercent = percent;
        }

        if (canSend) {
            eventProgress.postEvent(percent);
        }
    }

    protected final synchronized void refreshTaskTimeout() {
        if (taskState == STATE_RUNNING) {
            if (timeoutInterval > 0) {
                timeoutTimeStamp = System.currentTimeMillis() + timeoutInterval;
            }
        }
    }

    @Override
    public void abort() {
        // 只要不是已经挂起了，中断就只提请求，让当前执行的函数执行完成后判断任务是否已经中止。
        boolean taskPending = false;
        synchronized (this) {
            if (!isAborted) {
                if (taskState == STATE_RUNNING || taskState == STATE_STARTING) {
                    isAborted = true;
                    if (taskState == STATE_RUNNING) {
                        taskPending = this.taskPending;
                    }
                }
            }
        }
        // 已经处于挂起状态后，需要这里调用finish()进行清理
        if (taskPending) {
            // onCleanup() will clear callbacks to terminate this task.
            finished(ITaskResult.CODE_ABORT, new TaskError.Abort(this));
            // 如果taskPending为true那么，start()至少要在finish()结束后才能使用。所以，abort()和start()之间相互安全
        }
    }

    @Override
    public final void start(ITaskContext ctx, ITask prevTask) {

        final ILogger log = this.logger;
        TaskError errorOfInitParam;

        synchronized (this) {
            if (taskState != STATE_IDLE) {
                if (log != null) {
                    if (taskState == STATE_FINISHED) {
                        log.e(getName(), "The task is disposable and finished. It can NOT be started again. From " + CallUtil.trace(5));
                    } else {
                        log.w(getName(), "The task is not idle. DO NOT call start() again, from " + CallUtil.trace(5));
                    }
                }
                return;
            }

            taskState = STATE_STARTING; // 这一句之后，start()幂等性得到保障
            isFinished = false;
            isAborted = false;
            //didWork = false;
            lastPublishedPercent = -1;
            taskResultCode = 0;
            taskResultError = null; // 清理前一次的结果
            onStart(); // 子类在任务开始前初始化自身的状态。

            // 传入的参数不能是自己
            if (ctx != this) {
                rootCtx = ctx;
            }
            if (prevTask != this) {
                this.prevTask = prevTask;
            }

            // 获取任务中的参数
            errorOfInitParam = initializeParameter();
        }


        if (log != null && printVerboseLog) {
            log.v(getName(), "Started");
        }

        eventStart.postEvent(null);

        // 确保run被调度前，start事件已经发送
        synchronized (this) {
            taskState = STATE_RUNNING;
            hasRun = false;
        }

        if (errorOfInitParam != null) {
            finished(ITaskResult.CODE_ERROR, errorOfInitParam);
        } else if (isAborted) {
            finished(ITaskResult.CODE_ABORT, new TaskError.Abort(this)); // finish() 调用结束后才会恢复 IDLE，确保了start()的幂等性
        } else {
            // Start executing... It will call run().
            getExecutor().execute(this);
        }
    }

    @Override
    public final void run() {
        final ILogger log = this.logger;
        boolean aborted = false;

        // 判断是否要继续
        synchronized (this) {
            if (taskState != STATE_RUNNING) {
                if (log != null) {
                    log.w(getName(), "Task is not running. Unexpected executor schedule.");
                }
                return;
            }

            // 只允许被调度一次
            if (hasRun) {
                if (log != null) {
                    log.w(getName(), "Task is pending. Unexpected executor schedule.");
                }
                return;
            }

            hasRun = true; // 不管有没有被中止，

            // 任务挂起后，就不允许再执行doWork()了。因为只允许调度一次，所以这个条件应该永远不会成立。
            if (taskPending) {
                if (log != null) {
                    log.w(getName(), "Task is pending. Unexpected executor schedule.");
                }
                return;
            }

            // 运行前判断是否被终止
            if (isAborted) {
                aborted = true;
                // 在遇到任务中止的情况下，提前标记已经work了。避免finish()不能正常工作
                //didWork = true;
            }

        }

        if (aborted) {
            finished(ITaskResult.CODE_ABORT, new TaskError.Abort(this));
            return;
        }

        try {
            // 执行任务
            int timeoutMs = doWork();

            // 运行后判断是否被终止
            synchronized (this) {
                // 要为允许状态才继续，不为允许状态，后续的startTimer也没用
                if (taskState == STATE_RUNNING) {
                    if (isAborted) {
                        // 在这段代码之前调用了abort()，就不在执行挂起操作
                        // 在这段代码之中或之后调用了abort()，就由abort()来finish();
                        aborted = true;
                    } else {
                        // 判断是否需要挂起任务
                        if (timeoutMs > 0) {
                            taskPending = true;
                            timeoutInterval = timeoutMs;
                            timeoutTimeStamp = System.currentTimeMillis() + timeoutInterval;
                        }
                    }
                } else {
                    timeoutMs = 0; // 如果没有运行了，就不再挂起
                }
            }

            if (aborted) {
                finished(ITaskResult.CODE_ABORT, new TaskError.Abort(this));
                return;
            } else {
                if (timeoutMs > 0) {
                    startTimer(TIMER_ID_TASK_TIME_OUT, timeoutMs);
                    if (log != null && printVerboseLog) {
                        log.v(getName(), "Pend task for waiting some callbacks.");
                    }
                }
            }

            // 判断任务是否没有挂起也没有结束
            boolean autoFinish;
            synchronized (this) {
                autoFinish = taskState == STATE_RUNNING && !taskPending;
            }
            if (autoFinish) {
                if (log != null && printVerboseLog) {
                    log.v(getName(), "Call finished() automatically for sync task.");
                }

                finishedWithDone();
            }
        } catch (Throwable e) {
            //} catch (Exception e) { // can not catch Exception: FileNotFoundException, so that, try to catch Throwable.
            finishedWithError("Exception is occurred while running.", e);
        }
    }

    @Override
    public final synchronized <T> T getOutput(String key) {
        Object parameter = null;

        // then lookup from local
        if (envParameter != null) {
            parameter = envParameter.get(key);
        }

        // then lookup from previous task
        ITask prevTask = this.prevTask;
        if (parameter == null && prevTask != null) {
            this.prevTask = null; // 通过临时设置为null来避免递归导致的死循环
            parameter = prevTask.getOutput(key);
            this.prevTask = prevTask;
        }

        //noinspection unchecked
        return (T) parameter;
    }

    @Override
    public ITaskResult getResult() {
        return this;
    }

    @Override
    public <T> T getPreviousTask() {
        try {
            //noinspection unchecked
            return (T) prevTask;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public final synchronized <T> T getParameter(String key) {
        // the policy allow parent to overwrite the local parameter
        // get from parent firstly
        ITaskContext ctx = this.rootCtx;
        T parameter = null;
        if (ctx != null) {
            parameter = ctx.getParameter(key);
        }

        // then lookup from local
        if (parameter == null) {
            if (envParameter != null) {
                //noinspection unchecked
                parameter = (T) envParameter.get(key);
            }
        }
        return parameter;
    }

    @Override
    public final synchronized <T> void setParameter(String key, T val) {
        //only set self.
        // ITaskContext ctx = this.mRootCtx;
        //if (ctx != null) {
        //    ctx.setParameter(key, val);
        //}
        if (envParameter == null) {
            envParameter = new HashMap<>();
        }
        envParameter.put(key, val);
    }

    @Override
    public final Executor getExecutor() {
        Executor executor = mExecutor;
        // Get from parent firstly.
        // In general, it can always get executor from TaskQueue
        if (executor == null) {
            ITaskContext ctx = this.rootCtx;
            if (ctx != null) {
                executor = ctx.getExecutor();
            }
        } else {
            // if there is one, then skip rest judgment
            return executor;
        }

        // if not found, then
        if (executor == null) {
            // inherit from previous task
            ITask prevTask = this.prevTask;
            if (prevTask != null) {
                synchronized (this) {
                    this.prevTask = null; // 通过临时设置为null来避免递归导致的死循环
                    executor = prevTask.getExecutor();
                    this.prevTask = prevTask;
                }
            }
        } else {
            // if got one from ctx, then skip rest judgment
            return executor;
        }

        // if not found, then
        if (executor == null) {
            // use default executor
            synchronized (this) {
                if (mExecutor == null) {
                    mExecutor = TaskExecutor.getDefaultExecutor();
                }
                executor = mExecutor;
            }
        }

        // mExecutor = executor; 不需要保存从上下文获取到的Executor，避免重用Task时，上下文的Executor不会再次覆盖任务的

        return executor;
    }

    public boolean isStarted() {
        return taskState == STATE_RUNNING;
    }

    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public int getProgress() {
        int percent = this.lastPublishedPercent;
        if (percent < 0) {
            percent = 0;
        }
        return percent;
    }

    @SuppressWarnings({"Convert2Lambda", "UnusedReturnValue"})
    protected final synchronized TimerTask startTimer(final int id, long delay, long period) {
        if (taskState != STATE_RUNNING) {
            return null;
        }

        if (mTimer == null) {
            mTimer = new Timer();
        }

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinished) {
                            return;
                        }
                        if (id == TIMER_ID_TASK_TIME_OUT) {
                            if (System.currentTimeMillis() < timeoutTimeStamp) {
                                startTimer(TIMER_ID_TASK_TIME_OUT, timeoutTimeStamp - System.currentTimeMillis());
                            } else {
                                // taskPending = false; 由finish()去设置该标志
                                // provide a chance to subclass to overwrite the error code and error message.
                                onTaskExpired();
                                // 确保任务超时了，一定会finish
                                if (!isFinished) {
                                    finished(ITaskResult.CODE_TIMEOUT, new TaskError.Timeout(Task.this));
                                }
                            }
                        } else {
                            try {
                                onTimeout(id);
                            } catch (Exception e) {
                                finishedWithError("Exception in onTimer().", e);
                            }
                        }
                    }
                });
            }
        };
        if (period > 0) {
            mTimer.scheduleAtFixedRate(task, delay, period);
        } else {
            mTimer.schedule(task, delay);
        }
        return task;
    }

    @SuppressWarnings({"UnusedReturnValue"})
    protected final TimerTask startTimer(final int id, long delay) {
        return startTimer(id, delay, 0);
    }

    protected final synchronized void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // 由子任务实现的方法
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Do actual workload here.
     *
     * @return a timeout in millisecond. If less than or equal to zero, the task will cleanup after this method immediately.
     */
    protected abstract int doWork();

    /**
     * Subclass can do something before task starting in thread safe context.
     */
    protected void onStart() {
        // must be empty here
    }

    protected void onTimeout(int id) {
        // must be empty here
    }

    /**
     * Unregister callbacks and release resources immediately here.
     * Caution: the field may be NULL. this method may be called before the fields being initialized.
     */
    protected void onCleanup() {
        // must be empty here
    }

    /**
     * subclass can finish task with another error instead of TIME OUT error
     */
    protected void onTaskExpired() {
        // must be empty here
    }

    ///////////////////////////////////////////////////////////////////////////
    // 私有方法
    ///////////////////////////////////////////////////////////////////////////
    private TaskError initializeParameter() {
        Class clz = this.getClass();
        while (clz != null) {
            // 遍历类的每一个字段
            for (Field field : clz.getDeclaredFields()) {
                // 判断是否是任务参数
                TaskParameter annotation = field.getAnnotation(TaskParameter.class);
                if (annotation != null) {
                    try {
                        // look up context firstly
                        Object val = getParameter(field.getType().getName());
                        // then look up the output of previous task.
                        if (val == null) {
                            val = getOutput(field.getType().getName());
                        }

                        field.setAccessible(true);

                        if (val != null) {
                            field.set(this, val);
                            if (logger != null && printVerboseLog) {
                                logger.v(getName(), "Acquire parameter: " + field.getName() + " = " + val);
                            }
                        } else {
                            Object preVal = field.get(this);
                            if (preVal == null && !annotation.nullable()) {
                                return new TaskError(this, "Parameter " + field.getName() + " is null.");
                            }
                        }
                    } catch (IllegalAccessException e) {
                        return new TaskError(this, "Failed to set parameter " + field.getName(), e);
                    }
                }
            }
            // 遍历完 Task 的变量后，就不再往上级查找
            if (clz == Task.class) {
                break;
            }
            clz = clz.getSuperclass();
        }
        return null;
    }

    @Override
    public ITask getTask() {
        return this;
    }

    @Override
    public int getCode() {
        return taskResultCode;
    }

    @Override
    public TaskError getError() {
        return taskResultError;
    }

    protected final void finishedWithDone() {
        finished(ITaskResult.CODE_DONE, null);
    }

    protected final void finishedWithError(int resultCode, String errMsg) {
        finished(resultCode, new TaskError(this, errMsg));
    }

    protected final void finishedWithError(int resultCode, String errMsg, Throwable e) {
        finished(resultCode, new TaskError(this, errMsg, e));
    }

    protected final void finishedWithError(String errMsg) {
        finished(ITaskResult.CODE_ERROR, new TaskError(this, errMsg));
    }

    protected final void finishedWithError(String errMsg, Throwable e) {
        finished(ITaskResult.CODE_ERROR, new TaskError(this, errMsg, e));
    }
}
