package com.goodix.ble.libcomx.transceiver;


import com.goodix.ble.libcomx.util.HexReader;

public interface IFrameSdu4Rx {
    void deserialize(HexReader sdu);
}
