package com.goodix.ble.libcomx.ptmodel;

import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.task.ITask;
import com.goodix.ble.libcomx.task.ITaskResult;
import com.goodix.ble.libcomx.task.TaskQueue;

public final class PtStep extends TaskQueue {
    public static final int EVT_JUDGE_UPDATED = 728;
    private PtJudge judge;
    private Event<Void> eventJudgeUpdated = new Event<>(this, EVT_JUDGE_UPDATED);

    PtStep(PtJudge judge) {
        this.judge = judge;
        // Step 中的 Action 列表，只要出现一个异常就应该中止执行。
        setAbortOnException(true);
    }

    public PtJudge getJudge() {
        return judge;
    }

    public Event<Void> evtJudgeUpdated() {
        return eventJudgeUpdated;
    }

    public <T extends ITask> T addAction(T actonTask) {
        this.addTask(actonTask);
        return actonTask;
    }

    @Override
    protected void onCleanup() {
        super.onCleanup();
        // 将执行异常记录到测试结果中
        final ITaskResult result = getResult();
        if (result.getError() != null && this.judge != null) {
            this.judge.exception = result.getError().getMessage();
            this.evtJudgeUpdated().postEvent(null);
        }
    }
}
