package com.goodix.ble.libcomx.transceiver;

import com.goodix.ble.libcomx.util.HexBuilder;

/**
 * Create a frame and fill it with payload.
 * Similar to serialization.
 */
public interface IFrameBuilder {
    /**
     * Simply return how many bytes needed to build the frame.
     * @param type Frame type.
     * @param sdu Payload.
     * @return How many bytes needed to build the frame.
     */
    int calcFrameSize(int type, IFrameSdu4Tx sdu);

    /**
     * Wrap SDU to frame with the specified frame format.
     *
     * @param frame  put frame data to this PDU buffer, as output.
     * @param type Frame type.
     * @param sdu  Payload to be wrapped.
     */
    void buildFrame(HexBuilder frame, int type, IFrameSdu4Tx sdu);
}
