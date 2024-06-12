package com.goodix.ble.libcomx.transceiver.buffer;

import com.goodix.ble.libcomx.transceiver.IFrameSender;
import com.goodix.ble.libcomx.transceiver.buffer.ring.RingBuffer;

/**
 * Split a upper large PDU to some tiny lower PDU for transporting.
 */
public class BufferedPduSender implements IPduSegmentSender {

    private IFrameSender segmentSender;
    private RingBuffer buffer;
    private boolean sending;
    private int mtu;
    private byte[] innerBuffer;

    public BufferedPduSender(IFrameSender segmentSender) {
        this(segmentSender, 4096);
    }

    @SuppressWarnings("WeakerAccess")
    public BufferedPduSender(IFrameSender segmentSender, int cap) {
        this.mtu = 20;
        this.sending = false;
        this.segmentSender = segmentSender;
        this.buffer = new RingBuffer(cap);
        this.innerBuffer = new byte[this.mtu];
    }

    @Override
    public void setMaxSegmentSize(int size) {
        synchronized (this) {
            this.mtu = size;
            this.innerBuffer = new byte[this.mtu];
        }
    }

    @Override
    public void clear() {
        synchronized (this) {
            sending = false;
            buffer.dropAll();
        }
    }

    @Override
    public void nextSegment() {
        synchronized (this) {
            int actualLen = this.buffer.get(0, innerBuffer);
            if (actualLen > 0) {
                // copy to a new array
                byte[] buf = new byte[actualLen];
                System.arraycopy(innerBuffer, 0, buf, 0, actualLen);
                // send
                if (segmentSender.sendFrame(buf)) {
                    // remove the bytes have been sent
                    this.buffer.drop(actualLen);
                } else {
                    sending = false;
                    this.buffer.dropAll(); // clear all on error
                }
            } else {
                // no data to send
                sending = false;
            }
        }
    }

    @Override
    public boolean sendFrame(byte[] pdu) {
        if (pdu == null) return false;

        synchronized (this) {
            // judge space
            if (pdu.length <= this.buffer.getFreeSize()) {
                // append to save data
                this.buffer.put(pdu, 0, pdu.length);
                // trigger sending progress
                if (sending) {
                    return true;
                } else {
                    // fetch
                    int actualLen = this.buffer.get(0, innerBuffer);
                    if (actualLen > 0) {
                        // copy to a new array
                        byte[] buf = new byte[actualLen];
                        System.arraycopy(innerBuffer, 0, buf, 0, actualLen);
                        // send
                        if (segmentSender.sendFrame(buf)) {
                            sending = true;
                            // remove the bytes have been sent
                            this.buffer.drop(actualLen);
                            return true;
                        } else {
                            // clear all on error
                            // there is no need to send the remaining data when communication error
                            this.buffer.dropAll();
                            //return false;
                        }
                    } // else return false
                }
            }
        }

        return false;
    }

}
