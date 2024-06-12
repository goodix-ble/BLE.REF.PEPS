package com.goodix.ble.libcomx.transceiver;

import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.transceiver.buffer.ring.RingBuffer;
import com.goodix.ble.libcomx.util.HexBuilder;
import com.goodix.ble.libcomx.util.HexReader;

import java.util.Locale;

public class Transceiver implements ITransceiver {
    private static final String TAG = "Transceiver";

    private RingBuffer ringBuffer;

    private IFrameDetector detector;
    private IFrameParser sduRxParser;

    private IFrameBuilder frameBuilder;
    private IFrameSender sender;

    private Event<IFrameSdu4Rx> eventRcvFrame = new Event<>();
    private Event<Boolean> eventReady = new Event<>(this, EVT_READY);

    private ILogger logger;

    /**
     * 定义为字段，避免重复new
     */
    private IFrameDetector.ResultHolder resultHolder = new IFrameDetector.ResultHolder();
    private boolean readyToUse;

    public Transceiver(int bufferSize) {
        this.ringBuffer = new RingBuffer(bufferSize);
    }

    public Transceiver(int rxBufferSize, IFrameDetector detector, IFrameParser sduRxParser, IFrameBuilder creator, IFrameSender sender) {
        this.ringBuffer = new RingBuffer(rxBufferSize);
        this.detector = detector;
        this.sduRxParser = sduRxParser;
        this.frameBuilder = creator;
        this.sender = sender;
    }

    public Transceiver(RingBuffer ringBuffer, IFrameDetector detector, IFrameParser sduRxParser, IFrameBuilder creator, IFrameSender sender) {
        this.ringBuffer = ringBuffer;
        this.detector = detector;
        this.sduRxParser = sduRxParser;
        this.frameBuilder = creator;
        this.sender = sender;
    }

    public Transceiver setLogger(ILogger logger) {
        this.logger = logger;
        return this;
    }

    public void setReady(boolean ready) {
        final boolean readyToUse = this.readyToUse;
        if (readyToUse != ready) {
            this.readyToUse = ready;
            eventReady.postEvent(ready);
        }
    }

    public Transceiver setDetector(IFrameDetector detector) {
        this.detector = detector;
        return this;
    }

    public Transceiver setParser(IFrameParser sduRxParser) {
        this.sduRxParser = sduRxParser;
        return this;
    }

    public Transceiver setFrameBuilder(IFrameBuilder builder) {
        this.frameBuilder = builder;
        return this;
    }

    public Transceiver setSender(IFrameSender sender) {
        this.sender = sender;
        return this;
    }

    @Override
    public void handleRcvData(byte[] rcvData, int pos, int size) {
        if (rcvData == null) {
            if (logger != null) {
                logger.e(TAG, "rcv null pdu.");
            }
            return;
        }

        if (pos + size > rcvData.length) {
            int orgSize = size;
            size = rcvData.length - pos;
            if (logger != null) {
                logger.e(TAG, "the size of pdu is exceed pdu.length: pos: " + pos + "  size: " + orgSize + "  length: " + rcvData.length);
            }
        }

        printPdu("handle", rcvData, pos, size);

        // 防止多个线程同时调用
        synchronized (this) {
            // 还能放点儿数据进去，就放进去，然后提取有效帧
            int handledSize = ringBuffer.put(rcvData, pos, size);
            if (handledSize > 0) {
                detectFrame();
            }
            // 如果还有剩余没放进去的，那么就腾出空间，再塞进去
            if (handledSize < size) {
                pos += handledSize;
                size -= handledSize;
                ringBuffer.drop(size);
                if (logger != null) {
                    logger.w(TAG, "drop " + size + "  bytes pdu. remain " + ringBuffer.getSize() + " bytes data.");
                }
                ringBuffer.put(rcvData, pos, size);
                detectFrame();
            }
        }
    }

    @Override
    public boolean send(int type, IFrameSdu4Tx sdu) {
        if (frameBuilder != null && sender != null) {
            int size = frameBuilder.calcFrameSize(type, sdu);
            if (size > 0) {
                // TODO: 2019/8/15 可以优化为使用缓存池
                HexBuilder pdu = new HexBuilder(size);
                frameBuilder.buildFrame(pdu, type, sdu);
                printPdu("send", pdu.getBuffer(), 0, 0);
                return sender.sendFrame(pdu.getBuffer());
            }
        }
        return false;
    }

    @Override
    public boolean isReady() {
        return readyToUse;
    }

    @Override
    public Event<IFrameSdu4Rx> evtRcvFrame() {
        return eventRcvFrame;
    }

    @Override
    public Event<Boolean> evtReady() {
        return eventReady;
    }

    @Override
    public void reset() {
        if (logger != null) {
            logger.w(TAG, "reset RX buffer.");
        }
        synchronized (this) {
            ringBuffer.dropAll();
        }
    }

    /**
     * 榨干 buffer 中的帧
     */
    private void detectFrame() {
        while (detector.detectFrame(ringBuffer, resultHolder)) {
            // 拷贝出数据，移除pdu
            // TODO: 2019/8/15 可以优化为使用缓存池
            byte[] tmp = new byte[resultHolder.frameSize];
            int actLen = ringBuffer.pop(resultHolder.framePos, tmp);
            printPdu("detect", tmp, 0, -1);
            if (actLen != tmp.length) {
                if (logger != null) {
                    logger.e(TAG, "Error on getting pdu: expect=" + tmp.length
                            + "  actual=" + actLen
                            + "  pos=" + resultHolder.framePos);
                }
            }

            HexReader paramData = new HexReader(tmp);
            paramData.setRange(resultHolder.sduPos, resultHolder.sduSize);
            // 解析
            IFrameSdu4Rx sdu = sduRxParser.parseSdu(resultHolder.frameType, paramData);
            eventRcvFrame.postEvent(this, resultHolder.frameType, sdu);
        }
    }

    private void printPdu(String msg, byte[] pdu, int pos, int size) {
        if (logger != null) {
            StringBuilder sb = new StringBuilder(1024);
            if (size <= 0) size = pdu.length;
            sb.append(msg).append(" pdu[").append(size).append("]");
            for (int i = 0; i < pos + size; i++) {
                sb.append(String.format(Locale.US, "%02X ", 0xFF & pdu[pos + i]));
            }
            logger.v(TAG, sb.toString());
        }
    }
}
