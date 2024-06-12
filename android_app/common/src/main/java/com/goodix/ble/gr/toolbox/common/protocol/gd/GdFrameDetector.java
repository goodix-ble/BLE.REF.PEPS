package com.goodix.ble.gr.toolbox.common.protocol.gd;

import com.goodix.ble.libcomx.transceiver.IFrameDetector;
import com.goodix.ble.libcomx.transceiver.buffer.ring.IRingBuffer;

public class GdFrameDetector implements IFrameDetector {
    @Override
    public boolean detectFrame(IRingBuffer rcvData, ResultHolder result) {
        int size = rcvData.getSize();
        for (int i = 0; i < size; i++) {
            if (rcvData.get(i) == 0x44 && rcvData.get(i + 1) == 0x47) {
                // 没有足够的帧头
                if (i + 8 > size) {
                    i++;
                    continue;
                }
                // 解出帧头数据
                result.sduPos = 6; // 这里应该为相对位置
                result.sduSize = rcvData.get(i + 4) + (rcvData.get(i + 5) << 8);
                result.framePos = i;
                result.frameSize = 8 + result.sduSize;
                // 超出长度限制
                if (result.sduSize > 2 * 1024) {
                    i++;
                    continue;
                }
                // 没有足够的帧头
                if (i + result.frameSize > size) {
                    i++;
                    continue;
                }
                // 计算校验和
                int sum = 0;
                for (int k = 0; k < (result.sduSize + 2 + 2); k++) {
                    sum += rcvData.get(i + 2 + k);
                }
                sum = 0xFFFF & sum;
                int rcvSum = rcvData.get(i + 6 + result.sduSize) + (rcvData.get(i + 7 + result.sduSize) << 8);
                if (rcvSum != sum) {
                    i++;
                    continue;
                }
                // 找到完整的帧
                result.frameType = rcvData.get(i + 2) + (rcvData.get(i + 3) << 8);
                return true;
            }
        }
        return false;
    }
}
