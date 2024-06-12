package com.goodix.ble.libcomx.task;

import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.event.IEventListener;

/**
 * 用于将事件和任务连接起来。
 */
public class TaskJunction implements IEventListener {

    private Event srcEvt;
    private ITask nextTask;

    private boolean startIfError = false;
    private boolean startIfCode = false;
    private int expectTaskResultCode = ITaskResult.CODE_DONE;
    private boolean isOneshot;

    public static TaskJunction link(Event srcEvt, ITask nextTask) {
        TaskJunction chain = new TaskJunction();
        chain.srcEvt = srcEvt;
        chain.srcEvt.register(chain);
        chain.nextTask = nextTask;
        return chain;
    }

    public static TaskJunction link(ITask srcTask, ITask nextTask) {
        TaskJunction chain = new TaskJunction();
        chain.srcEvt = srcTask.evtFinished();
        chain.srcEvt.register(chain);
        chain.nextTask = nextTask;
        return chain;
    }

    private TaskJunction() {
    }

    public TaskJunction setStartIfError() {
        startIfError = true;
        return this;
    }

    public TaskJunction setStartIfCode(int expectTaskResultCode) {
        this.startIfCode = true;
        this.expectTaskResultCode = expectTaskResultCode;
        return this;
    }

    public TaskJunction setOneshot(boolean oneshot) {
        isOneshot = oneshot;
        return this;
    }

    public void destroy() {
        srcEvt.remove(this);
        srcEvt = null;
        nextTask = null;
    }

    /**
     * 用于接收触发当前任务执行的事件
     */
    @Deprecated
    @Override
    public void onEvent(Object src, int evtType, Object evtData) {
        // System.out.println("Receive event from [" + src.getClass().getSimpleName() + "], and start next task: " + nextTask.getName());

        // 尝试获取上下文
        ITask preTask = null;
        ITaskContext taskCtx = null;
        if (src instanceof ITask) {
            preTask = (ITask) src;
        } else if (src instanceof ITaskContext) {
            taskCtx = (ITaskContext) src;
        }

        boolean exec = true;
        if (evtType == ITask.EVT_FINISH && (evtData instanceof ITaskResult)) {
            ITaskResult result = (ITaskResult) evtData;
            if (startIfError) {
                exec = result.getError() != null;
            }
            if (startIfCode) {
                exec = result.getCode() == expectTaskResultCode;
            }
        }

        if (exec) {
            if (isOneshot) {
                destroy();
            }
            nextTask.start(taskCtx, preTask);
        }
    }
}
