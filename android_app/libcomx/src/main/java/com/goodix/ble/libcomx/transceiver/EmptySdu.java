package com.goodix.ble.libcomx.transceiver;

import com.goodix.ble.libcomx.util.HexBuilder;
import com.goodix.ble.libcomx.util.HexReader;

public class EmptySdu implements IFrameSdu4Tx, IFrameSdu4Rx {

    private static final EmptySdu INSTANCE = new EmptySdu();

    public static EmptySdu getInstance() {
        return INSTANCE;
    }

    @Override
    public void deserialize(HexReader sdu) {
    }

    @Override
    public int getSduSize() {
        return 0;
    }

    @Override
    public void serialize(HexBuilder sdu) {

    }
}
