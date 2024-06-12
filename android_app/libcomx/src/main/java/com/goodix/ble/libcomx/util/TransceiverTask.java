package com.goodix.ble.libcomx.util;

import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.EventDisposer;
import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libcomx.task.ITaskResult;
import com.goodix.ble.libcomx.task.Task;
import com.goodix.ble.libcomx.task.TaskError;
import com.goodix.ble.libcomx.task.TaskParameter;
import com.goodix.ble.libcomx.transceiver.IFrameSdu4Rx;
import com.goodix.ble.libcomx.transceiver.IFrameSdu4Tx;
import com.goodix.ble.libcomx.transceiver.ITransceiver;

import java.util.TimerTask;

public class TransceiverTask extends Task implements IEventListener {
    public static int defaultRetryCount = 0;
    public static int defaultRetryInterval = 0;

    @TaskParameter(nullable = true)
    private ITransceiver transceiver;

    // Overwrite the transceiver obtained from context.
    private ITransceiver specifiedTransceiver;

    private static final int SILENT_TIMER = 295;
    private int timeout = 3_000;
    private int silentTime = 0;
    private TimerTask silentTimer; // 防止重复创建计时器
    private int requestCode;
    private Integer expectResponseCode;
    private IFrameSdu4Tx request;
    private IFrameSdu4Rx response;
    private EventDisposer disposer = new EventDisposer();

    private static final int RETRY_TIMER = 293;
    private int maxRetry = defaultRetryCount;
    private int retryInterval = defaultRetryInterval;
    private int retryCnt;

    private ResponseHandler responseHandler;

    public interface ResponseHandler {
        void onRcvResponse(TransceiverTask task, int reqCode, IFrameSdu4Tx request, int respCode, IFrameSdu4Rx response) throws TaskError;
    }


    public TransceiverTask() {
    }

    public TransceiverTask(ITransceiver transceiver, int requestCode, IFrameSdu4Tx request, Integer expectResponseCode) {
        this(requestCode, request, expectResponseCode);
        setTransceiver(transceiver);
    }

    public TransceiverTask(int requestCode, IFrameSdu4Tx request, Integer expectResponseCode) {
        this.transceiver = null;
        this.requestCode = requestCode;
        this.expectResponseCode = expectResponseCode;
        this.request = request;
    }


    public TransceiverTask setTransceiver(ITransceiver transceiver) {
        this.specifiedTransceiver = transceiver;
        return this;
    }

    public TransceiverTask setTimeout(int timeout) {
        if (timeout > 0) {
            this.timeout = timeout;
        }
        return this;
    }

    public TransceiverTask setRequest(int requestCode, IFrameSdu4Tx request) {
        this.requestCode = requestCode;
        this.request = request;
        return this;
    }

    public TransceiverTask setExpectResponseCode(int expectResponseCode) {
        this.expectResponseCode = expectResponseCode;
        return this;
    }

    public TransceiverTask setHandler(ResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
        return this;
    }

    /**
     * In case of sending a special command which should not be acknowledged.
     * The task will success when it runs out of silent time.
     */
    public TransceiverTask setSilentTime(int millis) {
        this.silentTime = millis;
        return this;
    }

    public <T extends IFrameSdu4Rx> T getResponse() {
        //noinspection unchecked
        return (T) response;
    }

    public TransceiverTask setRetry(int maxRetry, int retryInterval) {
        this.maxRetry = maxRetry;
        this.retryInterval = retryInterval;
        if (this.timeout <= retryInterval) {
            this.timeout = retryInterval + 1;
        }
        return this;
    }

    @Override
    protected int doWork() {
        if (specifiedTransceiver != null) {
            transceiver = specifiedTransceiver;
        }

        if (transceiver == null) {
            finishedWithError("Transceiver is null.");
            return 0;
        }

        if (expectResponseCode != null) {
            transceiver.evtRcvFrame()
                    .subEvent(this).setDisposer(disposer).setExecutor(getExecutor()).register(this);
        }
        // 监听通信恢复事件
        transceiver.evtReady()
                .subEvent(this).setDisposer(disposer).setExecutor(getExecutor()).register(this);

        retryCnt = 0;
        silentTimer = null;

        int timeout = this.timeout;
        if (maxRetry > 0) {
            int totalTimeForRetry = retryInterval * (maxRetry + 1);
            //noinspection ManualMinMaxCalculation
            timeout = totalTimeForRetry > timeout ? totalTimeForRetry : timeout;
            // 启动起一个重试Timer
            startTimer(RETRY_TIMER, retryInterval);
        }

        sendCmd();

        return timeout;
    }

    @Override
    protected void onCleanup() {
        super.onCleanup();
        disposer.disposeAll(this);
    }

    @Override
    protected void onTimeout(int id) {
        if (id == RETRY_TIMER) {
            retryCnt++;
            // ignore last interval. timeout timer will check time.
            if (retryCnt < maxRetry) {
                startTimer(RETRY_TIMER, retryInterval);
            }
            sendCmd();
            return;
        }

        if (id == SILENT_TIMER) {
            finishedWithDone();
            //return;
        }
    }

    @Override
    public void onEvent(Object src, int evtType, Object evtData) {
        if (src == transceiver) {
            if (evtType == expectResponseCode) {
                response = (IFrameSdu4Rx) evtData;
                try {
                    ResponseHandler handler = this.responseHandler;
                    if (handler != null) {
                        handler.onRcvResponse(this, requestCode, request, evtType, response);
                    }
                    finishedWithDone();
                } catch (TaskError taskError) {
                    finished(ITaskResult.CODE_ERROR, taskError);
                }
            } else if (evtType == ITransceiver.EVT_READY && evtData instanceof Boolean) {
                boolean ready = (boolean) evtData;
                // 如果没有retry，就没有等待的价值
                if (!ready) {
                    if (maxRetry <= 0) {
                        finishedWithError("Transceiver has turned into unavailable.");
                    }
                }
            }
        }
    }

    private void sendCmd() {
        final ILogger log = logger;

        // output warning message for retry.
        if (log != null) {
            if (maxRetry > 0) {
                String tryMsg = "Retry #" + retryCnt;
                if (retryCnt > 1) {
                    log.w(getName(), tryMsg);
                } else if (printVerboseLog) {
                    // first try
                    log.v(getName(), tryMsg);
                }
            }
        }

        // send cmd anyway.
        boolean notSent = true;
        if (transceiver.isReady()) {
            if (transceiver.send(requestCode, request)) {
                notSent = false;
                if (expectResponseCode == null) {
                    finishedWithDone();
                } else if (silentTime > 0) {
                    if (silentTimer == null) {
                        silentTimer = startTimer(SILENT_TIMER, silentTime);
                    } else {
                        if (log != null) {
                            log.w(getName(), "Silent Timer is already existed: " + silentTimer);
                        }
                    }
                }
            } else {
                if (log != null) {
                    log.w(getName(), "Failed to send: " + requestCode);
                }
            }
        } else {
            if (log != null) {
                log.w(getName(), "Transceiver is not ready.");
            }
        }
        // 没有发送成功，又没有重试的机会，就直接报错
        if (notSent && maxRetry <= 0) {
            finishedWithError("Failed to send: " + requestCode);
        }
    }
}
