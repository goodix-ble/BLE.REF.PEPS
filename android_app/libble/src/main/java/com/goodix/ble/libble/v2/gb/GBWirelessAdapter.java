package com.goodix.ble.libble.v2.gb;

import com.goodix.ble.libble.v2.gb.procedure.GBProcedure;
import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.Event;

import java.util.List;

/**
 * 主要表示本机的蓝牙设备，该蓝牙设备可以支持中心设备和外围设备
 * 作为中心设备角色：可以扫描并创建远程设备
 * 作为外设备角色：可以创建虚拟设备
 */
public interface GBWirelessAdapter {
    Integer STATE_INIT = 1;
    Integer STATE_ENABLED = 2;
    Integer STATE_DEINIT = 3;
    Integer STATE_DISABLED = 4;

    void setLogger(ILogger logger);

    /**
     * 判断适配器是否可用
     */
    boolean isEnabled();

    /**
     * 控制是否让控制器工作
     */
    GBProcedure setEnable(boolean enable);

    /**
     * 复位适配器的状态。复位成功后，其产生的控制类可能也会受到影响而不能正常工作。
     * 复位时状态会经历 DEINIT -> INIT -> ENABLED
     *
     * @param toFactory 是否重置到出厂状态：
     *                  true - 丢掉所有存储的信息（例如：绑定信息）
     *                  false - 仅重置一些临时的运行时工作状态
     */
    GBProcedure reset(boolean toFactory);

    /**
     * 通知工作状态的变更
     */
    Event<Integer> evtStateChanged();

    /**
     * 用于上报错误信息
     */
    Event<String> evtError();

    GBScanner createScanner();

    GBAdvertiser createAdvertiser();

    GBRemoteDevice createDevice(String mac); // 一般是通过Scanner来获得

    GBPeripheral createPeripheral(String mac);

    List<GBRemoteDevice> getBondDevices();
}
