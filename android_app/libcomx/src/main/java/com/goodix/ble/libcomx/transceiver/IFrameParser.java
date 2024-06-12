package com.goodix.ble.libcomx.transceiver;

import com.goodix.ble.libcomx.util.HexReader;

/**
 * 该接口实现了对接收数据的解析。
 * 将收到数据SDU数据按照type的值进行解析，并创建一个具体的对象来存储解析结果。
 * 最后将这个对象返回。
 * 这个过程类似于反序列化。
 * <p>
 * 可以在实现类中加入Event机制，实现对某一类数据接收事件的监听。
 * 例如：
 * <code>
 * Event<CmdStartup> startupEvent = new Event<>(IFrameParser, EVT_CMD_STARTUP);
 * </code>
 */
public interface IFrameParser {
    /**
     * 反序列化出负载数据实体
     *
     * @param type 帧类型，或命令类型。
     * @param sdu  数据字节流。通过 {@link HexReader#getBuffer()} 可以获得原始的帧数据。
     * @return 负载数据实体，或命令类实例。
     */
    IFrameSdu4Rx parseSdu(int type, HexReader sdu);
}
