package com.goodix.ble.libcomx.transceiver.buffer;

import com.goodix.ble.libcomx.transceiver.IFrameSender;

public interface IPduSegmentSender extends IFrameSender {

    void setMaxSegmentSize(int size);

    void clear();

    void nextSegment();
}
