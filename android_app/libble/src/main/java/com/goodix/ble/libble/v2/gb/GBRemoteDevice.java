package com.goodix.ble.libble.v2.gb;

import com.goodix.ble.libble.v2.gb.gatt.GBGattService;
import com.goodix.ble.libble.v2.gb.pojo.GBCI;
import com.goodix.ble.libble.v2.gb.pojo.GBError;
import com.goodix.ble.libble.v2.gb.pojo.GBPhy;
import com.goodix.ble.libble.v2.gb.procedure.GBProcedure;
import com.goodix.ble.libble.v2.gb.procedure.GBProcedureConnect;
import com.goodix.ble.libble.v2.gb.procedure.GBProcedureRssiRead;
import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.task.TaskQueue;

import java.util.List;
import java.util.UUID;

/**
 * 该接口定义了对远程设备的全部操作。
 * <p>
 * 未来扩展：
 * 1. 记录并提供一系列的广播数据，可用于广播数据追踪分析或绘图展示
 */
public interface GBRemoteDevice {
    int STATE_DISCONNECTED = 0;
    int STATE_CONNECTING = 1;
    int STATE_CONNECTED = 2;
    int STATE_DISCONNECTING = 3;

    int EVT_CI_UPDATED = 101;
    int EVT_ERROR = 102;
    int EVT_MTU_UPDATED = 103;
    int EVT_PHY_UPDATED = 104;
    int EVT_STATE_CHANGED = 106;
    int EVT_READY = 108;

    ILogger getLogger();

    void setLogger(ILogger logger);

    /**
     * 获取设备的名称，如果不可用，就返回 "N/A"
     *
     * @return "N/A" if not available.
     */
    String getName();

    String getAddress();

    int getState();

    boolean isConnected();

    boolean isDisconnected();

    boolean isBond();

    int getMtu();

    /**
     * 获得当前已知的PHY的设置情况，
     * 可能为无效值，通过 {@link #readCurrentPhy()} 可以更新它
     *
     * @return 非 NULL 返回值
     */
    GBPhy getPhy();

    /**
     * 获得当前已知的连接参数的设置情况，
     *
     * @return 非 NULL 返回值
     */
    GBCI getConnectionParameter();

    /**
     * 获取设备的初始化队列，在设备成功连接后，初始化队列的内容就会执行。队列的内容执行完成并且没有错误时，会触发{@link #evtReady()}。
     * 可以在队列中加入需要的规程，例如发现服务、设置MTU等。
     *
     * @return 任务队列，非空。
     */
    TaskQueue getSetupSteps();

    /**
     * 判断设备是否完成了初始化动作
     *
     * @return true - 已完成初始化，可以使用了。 false - 未完成初始化，使用可能会导致异常
     */
    boolean isReady();

    /**
     * 判断是否在使用中。
     * 从{@link #connect(int)}被执行开始，设备就属于“正在使用”，表示用户需要连接。
     * 从{@link #disconnect(boolean)}或{@link #removeBond()}被执行后，释放连接，表示用户不需要连接。
     *
     * @return true - 连接资源被使用中。 false - 已不需要连接资源
     */
    boolean isInService();

    ///////////////////////////////////////////////////////////////////////////
    // Event
    ///////////////////////////////////////////////////////////////////////////

    Event<Integer> evtStateChanged();

    Event<Integer> evtMtuUpdated();

    Event<GBPhy> evtPhyUpdated();

    Event<GBCI> evtCIUpdated();

    Event<GBError> evtError();

    Event<Boolean> evtReady();

    void clearEventListener(Object tag);

    ///////////////////////////////////////////////////////////////////////////
    // manage procedures
    ///////////////////////////////////////////////////////////////////////////
    void clearPendingProcedure();

    ///////////////////////////////////////////////////////////////////////////
    // Other procedure
    ///////////////////////////////////////////////////////////////////////////

    GBProcedureConnect connect(int preferedPhy);

    GBProcedure disconnect(boolean clearCache);

    GBProcedure discoverServices();

    GBProcedureRssiRead readRemoteRssi();

    GBProcedure setPreferredPhy(int txPhy, int rxPhy, int phyOptions);

    GBProcedure readCurrentPhy();

    /**
     * @param connectionPriority see {@link GBCI}
     * @return
     */
    GBProcedure setConnectionPriority(int connectionPriority);

    GBProcedure setMtu(int mtu);

    GBProcedure createBond();

    GBProcedure removeBond(); // Android does not support it now.

    ///////////////////////////////////////////////////////////////////////////
    // GATT Management
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 判断当前连接是否已经完成了服务发现规程
     */
    boolean isDiscovered();

    /**
     * 定义一个服务，不管服务是否已经存在
     *
     * @param uuid      服务的UUID
     * @param mandatory 该服务是否为必须的服务。当为true时，如果发现服务规程没有找到该服务，就会报错。
     * @return 服务的实例
     */
    GBGattService defineService(final UUID uuid, boolean mandatory);

    /**
     * 返回一个已经存在的服务，如果服务不存在，就通过{@link #defineService(UUID, boolean)}定义一个服务
     */
    GBGattService requireService(final UUID uuid, boolean mandatory);

    List<GBGattService> getService(final UUID uuid);

    List<GBGattService> getServices();
}
