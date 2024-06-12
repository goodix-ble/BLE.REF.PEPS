package com.goodix.ble.libcomx.transceiver;

import com.goodix.ble.libcomx.event.Event;

public interface ITransceiver {
    int EVT_READY = 0xEFA8EAD1;

    void handleRcvData(byte[] rcvData, int pos, int size);

    boolean send(int type, IFrameSdu4Tx sdu);

    boolean isReady();

    Event<IFrameSdu4Rx> evtRcvFrame();

    Event<Boolean> evtReady();

    /**
     * Reset state and clear buffers.
     */
    void reset();
}
