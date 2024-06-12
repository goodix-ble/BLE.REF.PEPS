package com.goodix.ble.gr.toolbox.common.protocol.gd;

import com.goodix.ble.libcomx.transceiver.IFrameBuilder;
import com.goodix.ble.libcomx.transceiver.IFrameSdu4Tx;
import com.goodix.ble.libcomx.util.HexBuilder;

public class GdFrameBuilder implements IFrameBuilder {
    @Override
    public int calcFrameSize(int type, IFrameSdu4Tx sdu) {
        return 8 + sdu.getSduSize();
    }

    @Override
    public void buildFrame(HexBuilder frame, int type, IFrameSdu4Tx sdu) {
        // magic number
        frame.put(0x4744, 2);
        // type
        frame.put(type, 2);
        // length
        int sduSize = sdu.getSduSize();
        frame.put(sduSize, 2);
        // sdu
        frame.setRange(2 + 2 + 2, sduSize); // 限制pdu能写入数据的范围
        frame.setPos(0);
        sdu.serialize(frame);
        // calcChecksum
        int check = calcChecksum(frame.getBuffer(), 2, 2 + 2 + sduSize);
        frame.setRange(2 + 2 + 2 + sduSize, 2);
        frame.setPos(0);
        frame.put(check, 2);
    }

    public static int calcChecksum(byte[] dat, int pos, int size) {
        int sum = 0;
        if (pos + size > dat.length) size = dat.length - pos;
        for (int i = 0; i < size; i++) {
            sum += (0xFF & dat[pos]);
            pos++;
        }
        return sum;
    }
}
