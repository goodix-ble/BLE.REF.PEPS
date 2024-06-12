package com.goodix.ble.libcomx.transceiver;

import com.goodix.ble.libcomx.transceiver.buffer.ring.IRingBuffer;

public interface IFrameDetector {
    class ResultHolder {
        public int framePos; // 帧在pdu中的起始位置
        public int frameSize; // 帧的大小
        public int frameType; // 帧类型，这个会决定用什么解析类来解析参数
        public int sduPos; // 参数在pdu中的起始位置，相对位置，从0开始
        public int sduSize; // 参数的大小
    }

    /**
     * 检测pdu中的帧，并且将帧数据拷贝到一个缓存中。
     *
     * @param rcvData    接收到的原始数据
     * @param result 用于输出检测到的结果
     * @return true-表示检测到了帧
     */
    boolean detectFrame(IRingBuffer rcvData, ResultHolder result);
}
