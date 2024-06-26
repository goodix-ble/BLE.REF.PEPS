package com.goodix.ble.libble.v2.gb.procedure;

import com.goodix.ble.libcomx.task.ITask;

public interface GBProcedure extends ITask {
    /**
     * 启动规程，如果当前系统处于占用状态，就会将该规程加入等待队列中。
     * 系统被释放占用后，自动开始执行。
     */
    void startProcedure();

    GBProcedure setTimeout(int timeout);
}
