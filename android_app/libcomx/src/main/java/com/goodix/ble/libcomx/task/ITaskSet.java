package com.goodix.ble.libcomx.task;

import com.goodix.ble.libcomx.event.Event;

/**
 * 定义任务集合的接口，将多个子任务组合起来，完成更复杂的任务。
 * 可以有一下两种实现：
 * 1. 任务队列：将一系列子任务作为一个整体依次执行。子任务之间有前后的逻辑关联性，一个失败，整体就失败。
 * 2. 任务管道：依次执行添加的子任务，子任务之间有前后没有关联性。对于要范围有限资源的任务有用
 * 3. 任务网：用于构建和管理多个子任务之间的衔接关系，形成关系网，例如A执行失败时执行B，执行成功时执行C。
 */
public interface ITaskSet extends ITask {
    int EVT_SUBTASK_START = 821;
    int EVT_SUBTASK_PROGRESS = 822;
    int EVT_SUBTASK_FINISH = 823;


    Event<ITask> evtSubtaskStart();

    Event<ITask> evtSubtaskProgress();

    Event<ITask> evtSubtaskFinish();
}
