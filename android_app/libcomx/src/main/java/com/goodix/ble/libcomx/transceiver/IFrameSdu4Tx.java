package com.goodix.ble.libcomx.transceiver;

import com.goodix.ble.libcomx.util.HexBuilder;

public interface IFrameSdu4Tx {
    /**
     * 预先获取SDU的大小，以便分配空间
     */
    int getSduSize();

    void serialize(HexBuilder sdu);
}
