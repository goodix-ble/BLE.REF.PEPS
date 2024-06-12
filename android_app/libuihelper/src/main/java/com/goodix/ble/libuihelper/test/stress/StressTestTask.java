package com.goodix.ble.libuihelper.test.stress;

import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.event.EventDisposer;
import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libcomx.task.ITask;
import com.goodix.ble.libcomx.task.ITaskResult;
import com.goodix.ble.libcomx.task.ITaskSet;
import com.goodix.ble.libcomx.task.Task;

public class StressTestTask extends Task implements IEventListener {

    public static final int EVT_STATUS = 920231;
    public static final int EVT_TEST_ERROR = 920232;

    private Event<String> eventStatus = new Event<>(this, EVT_STATUS);
    private Event<StressTestTask.Failure> eventTestError = new Event<>(this, EVT_TEST_ERROR);

    private int taskTimeout = 120_000;
    private int totalCount;
    private int maxFail;
    private int maxHardFail; // max continuous fail
    private int maxTime;

    private int testCount;
    private int failCount; // second
    private int hardFailCount; // counter of continuous fail
    private long startTimestamp;
    private long stopTimestamp;
    private boolean requestedStop = false;

    private ITask testTask = null;
    private ITask setUpTask = null;
    private ITask tearDownTask = null;
    protected EventDisposer disposer = new EventDisposer();

    public Event<String> evtStatus() {
        return eventStatus;
    }

    public Event<Failure> evtTestError() {
        return eventTestError;
    }

    public void setConfig(int totalCount, int maxFail, int maxTime) {
        this.totalCount = totalCount;
        this.maxFail = maxFail;
        this.maxTime = maxTime;
    }

    public void setMaxHardFail(int maxHardFail) {
        this.maxHardFail = maxHardFail;
    }

    public void setTestTask(ITask testTask) {
        if (isStarted()) {
            throw new IllegalStateException("Test is flying.");
        }
        this.testTask = testTask;
    }

    public void setSetUpTask(ITask setUpTask) {
        this.setUpTask = setUpTask;
    }

    public void setTearDownTask(ITask tearDownTask) {
        this.tearDownTask = tearDownTask;
    }

    public void setTimeout(int timeout) {
        this.taskTimeout = timeout;
    }

    public void stopTest() {
        if (requestedStop) {
            abort();
        } else {
            requestedStop = true;
            if (tearDownTask == null) {
                abort();
            } else {
                if (setUpTask != null && setUpTask.isStarted()) {
                    setUpTask.abort();
                }
                if (testTask != null) {
                    testTask.abort();
                }
                finishTest();
            }
        }
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getMaxFail() {
        return maxFail;
    }

    public int getTestCount() {
        return testCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getStopTimestamp() {
        return stopTimestamp;
    }

    public ITask getTestTask() {
        return testTask;
    }

    public ITask getSetUpTask() {
        return setUpTask;
    }

    public ITask getTearDownTask() {
        return tearDownTask;
    }

    @Override
    public ITask setLogger(ILogger logger) {
        super.setLogger(logger);
        synchronized (this) {
            ITask task = this.testTask;
            if (task != null) {
                task.setLogger(logger);
            }
        }
        return this;
    }

    @Override
    public ITask setDebug(boolean enable) {
        synchronized (this) {
            ITask task = this.testTask;
            if (task != null) {
                task.setDebug(enable);
            }
        }
        return super.setDebug(enable);
    }

    @Override
    protected final int doWork() {

        testTask = onCreateTest();

        startTimestamp = System.currentTimeMillis();
        testCount = 0;
        failCount = 0;
        hardFailCount = 0;
        stopTimestamp = 0;
        requestedStop = false;
        disposer.disposeAll(null);

        if (testTask == null) {
            finishedWithError("Test is null. please check onCreateTest()");
        } else {
            testTask.setLogger(logger);
            testTask.setDebug(printVerboseLog);

            // 在Finish事件中启动新一轮测试，此时自动清除listener的功能可能会清除刚刚添加进去的listener，导致循环链断裂
            // 所以，不在启动新一轮测试的时候每次都去设置listener，只在本任务启动的时候设置一次，避免AutoClearListener因为线程安全导致刚添加进去的listener被清除掉
            //testTask.setOneshot(false);
            //testTask.evtStart().subEvent(this).setExecutor(getExecutor()).register(this); 不需要
            //testTask.evtProgress().subEvent(this).setExecutor(getExecutor()).register(this); 不需要，进度由测试次数来计算
            testTask.evtFinished().subEvent(this).setExecutor(getExecutor()).register(this);
            if (testTask instanceof ITaskSet) {
                ((ITaskSet) testTask).evtSubtaskStart().subEvent(this).setExecutor(getExecutor()).register(this);
                ((ITaskSet) testTask).evtSubtaskProgress().subEvent(this).setExecutor(getExecutor()).register(this);
            }

            if (tearDownTask != null) {
                tearDownTask.evtStart().register(this);
                tearDownTask.evtFinished().register(this);
                tearDownTask.setLogger(logger);
                tearDownTask.setDebug(printVerboseLog);
            }

            ITask setUpTask = this.setUpTask;
            if (setUpTask != null) {
                setUpTask.evtStart().register(this);
                setUpTask.evtFinished().register(this);
                setUpTask.setLogger(logger);
                setUpTask.setDebug(printVerboseLog);

                final ILogger log = logger;
                if (log != null) {
                    log.i(getName(), "Set up test: " + setUpTask.getName());
                }

                setUpTask.start(this, null);

            } else {
                tryStartTest();
            }
        }
        return taskTimeout;
    }

    protected ITask onCreateTest() {
        return testTask;
    }

    protected boolean onStartTest(ITask testTask) {
        testTask.setName(getName() + "_" + (testCount + 1));
        testTask.start(this, setUpTask);
        return true;
    }

    @Override
    protected void onCleanup() {
        super.onCleanup();
        disposer.disposeAll(null);
        stopTimestamp = System.currentTimeMillis();

        if (setUpTask != null) {
            setUpTask.clearListener(this);
            setUpTask.abort();
            setUpTask = null;
        }

        if (testTask != null) {
            testTask.clearListener(this);
            // 先清除listener再abort，避免abort的时候，又启动了新一轮的测试
            testTask.abort();
        }

        if (tearDownTask != null) {
            tearDownTask.clearListener(this);
            tearDownTask.abort();
            tearDownTask = null;
        }

        eventStatus.clear();
        eventTestError.clear();
    }

    @Override
    public void onEvent(Object src, int evtType, Object evtData) {
        final ILogger log = logger;

        if (evtType == ITaskSet.EVT_SUBTASK_START) {
            refreshTaskTimeout();
            ITask parentTask = (ITask) src;
            ITask subTask = (ITask) evtData;
            String name = subTask.getName();
            if (parentTask != testTask) {
                name = parentTask.getName() + " -> " + subTask.getName();
            }
            evtStatus().postEvent(name);
            if (subTask instanceof ITaskSet) {
                ((ITaskSet) subTask).evtSubtaskStart().subEvent(this).setExecutor(getExecutor()).register(this);
                subTask.evtFinished().subEvent(this).setExecutor(getExecutor()).register(this);
            }

        } else if (evtType == ITask.EVT_START) {
            refreshTaskTimeout();
            evtStatus().postEvent(((ITask) src).getName());

        } else if (evtType == ITaskSet.EVT_SUBTASK_PROGRESS) {
            if (src == testTask) {
                refreshTaskTimeout();
            } else {
                if (log != null) {
                    log.w(getName(), "unkown EVT_SUBTASK_PROGRESS of: " + src);
                }
            }

        } else if (evtType == ITask.EVT_FINISH) {
            ITaskResult result = (ITaskResult) evtData;
            if (src == testTask) {
                // next test
                refreshTaskTimeout();

                // 统计结果
                if (result.getError() == null) {
                    int percent = 0;
                    hardFailCount = 0;
                    if (totalCount > 0) {
                        percent = testCount * 100 / totalCount;
                    }
                    publishProgress(percent);
                } else {
                    // abort错误不计入
                    if (result.getCode() != ITaskResult.CODE_ABORT) {
                        failCount++;
                        hardFailCount++;
                        Failure info = new Failure();
                        info.testNumber = testCount;
                        info.failNumber = failCount;
                        info.timestamp = System.currentTimeMillis();
                        info.msg = "Error: " + result.getError().getRootCause().getMessage();
                        eventTestError.postEvent(info);

                        if (maxHardFail > 0 && hardFailCount >= maxHardFail) {
                            finishedWithError(ITaskResult.CODE_ERROR, "Encountered hard failure.", result.getError());
                        }
                    }
                }

                tryStartTest();
            } else if (src == setUpTask) {
                if (result.getError() == null) {
                    tryStartTest();
                } else {
                    if (result.getCode() != ITaskResult.CODE_ABORT) {
                        finishedWithError(ITaskResult.CODE_ERROR, "Failed to set up.", result.getError());
                    }
                }

            } else if (src == tearDownTask) {
                if (isStarted()) {
                    if (requestedStop) {
                        abort();
                    } else {
                        finishedWithDone();
                    }
                }

            } else {
                if (src instanceof ITask) {
                    ((ITask) src).clearListener(this);
                }
            }

        } else {
            if (log != null) {
                log.w(getName(), "unkown event: " + evtType + " src: " + src);
            }
        }
    }

    private void tryStartTest() {
        final ILogger log = logger;

        // 如果已经结束了就不再进行判断和处理了
        if (isAborted || requestedStop) {
            return;
        }

        // 次数到了，停止
        if (testCount >= totalCount) {
            finishTest();
            return;
        }

        // 过多错误，停止
        if (failCount > maxFail) {
            finishTest();
            return;
        }

        // 测试了足够长时间
        if (maxTime > 0 && (System.currentTimeMillis() - startTimestamp) >= maxTime) {
            finishTest();
            return;
        }

        String msg = "Started: #" + (testCount + 1);
        evtStatus().postEvent(msg);
        if (log != null) {
            log.v(getName(), msg);
        }
        if (onStartTest(testTask)) {
            testCount++;
        } else {
            finishedWithError("Failed to start test#" + (testCount + 1));
        }
    }

    private void finishTest() {
        // 通过TearDown结束任务，或者直接结束任务
        ITask tearDownTask = this.tearDownTask;
        if (tearDownTask != null) {
            final ILogger log = logger;
            if (log != null) {
                log.i(getName(), "Tear down test: " + tearDownTask.getName());
            }
            tearDownTask.start(this, testTask);

        } else {
            finishedWithDone();
        }
    }

    public static class Failure {
        public int testNumber;
        public int failNumber;
        public long timestamp;
        public String msg;
    }
}
