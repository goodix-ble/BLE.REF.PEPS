package com.goodix.ble.libble.v2.misc;

import android.bluetooth.BluetoothGattCharacteristic;

import com.goodix.ble.libble.BleUuid;
import com.goodix.ble.libble.v2.gb.GBRemoteDevice;
import com.goodix.ble.libble.v2.gb.gatt.GBGattCharacteristic;
import com.goodix.ble.libble.v2.gb.gatt.GBGattDescriptor;
import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libcomx.transceiver.IFrameSender;
import com.goodix.ble.libcomx.transceiver.Transceiver;
import com.goodix.ble.libcomx.transceiver.buffer.BufferedPduSender;

/**
 * 用于将 Transceiver 和 GBGattCharacteristic 关联起来，
 * 方便通过用户程序通过特性进行数据的收发。
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class BleTransceiver implements IEventListener, IFrameSender {

    private Transceiver boundTransceiver;
    private IFrameSender pduSender;
    private BufferedPduSender bufferedPduSender;

    private GBGattCharacteristic gbCharacteristic;
    private GBGattDescriptor gbCharacteristicCCCD;

    public BleTransceiver(GBGattCharacteristic characteristic) {
        this.gbCharacteristic = characteristic;
        this.gbCharacteristicCCCD = null;

        // it fails if device is not connected.
        //List<GBGattDescriptor> descriptor = gbCharacteristic.getDescriptor(BleUuid.CCCD);
        //if (!descriptor.isEmpty()) {
        //    gbCharacteristicCCCD = descriptor.get(0);
        //}

        gbCharacteristicCCCD = gbCharacteristic.requireDescriptor(BleUuid.CCCD, false);
    }

    public Transceiver bindTransceiver(Transceiver transceiver) {
        return bindTransceiver(transceiver, 0);
    }

    public Transceiver bindTransceiver(Transceiver transceiver, int bufferSizeOfSender) {
        // remove previous reference to this sender
        if (this.boundTransceiver != null) {
            this.boundTransceiver.setSender(null);
        }
        this.boundTransceiver = transceiver;
        // 监听要接收的数据
        gbCharacteristic.evtNotify().register(this);
        gbCharacteristic.evtIndicate().register(this);

        // bind to this sender
        IFrameSender sender;
        if (bufferSizeOfSender > 20) {
            sender = getBufferedFrameSender(bufferSizeOfSender);
        } else {
            // observe connection changes: reset rx buffer when connecting
            // add if absent
            gbCharacteristic.getService().getRemoteDevice().evtStateChanged().register(this);
            sender = this;
        }
        transceiver.setSender(sender);

        // observe if notification is enabled.
        if (gbCharacteristicCCCD != null) {
            gbCharacteristicCCCD.evtRead().register(this);
            gbCharacteristicCCCD.evtWritten().register(this);
        }

        // initialize the ready state
        transceiver.setReady(gbCharacteristic.getService().getRemoteDevice().isConnected() && (gbCharacteristic.isNotifyEnabled() || gbCharacteristic.isIndicateEnabled()));

        return transceiver;
    }

    public IFrameSender getFrameSender() {
        return this;
    }

    public IFrameSender getBufferedFrameSender(int bufferSize) {
        if (bufferedPduSender == null) {
            bufferedPduSender = new BufferedPduSender(this, bufferSize);
            GBRemoteDevice remoteDevice = gbCharacteristic.getService().getRemoteDevice();
            // observe written event.
            gbCharacteristic.evtWritten().register(this);
            // observe connection changes: reset buffer
            remoteDevice.evtStateChanged().register(this);
            // observe MTU changes
            remoteDevice.evtMtuUpdated().register(this);
        }
        return bufferedPduSender;
    }

    public void unbind() {
        GBRemoteDevice remoteDevice = gbCharacteristic.getService().getRemoteDevice();
        if (this.boundTransceiver != null) {
            this.boundTransceiver.setSender(null);
            this.boundTransceiver = null;
            gbCharacteristic.evtNotify().remove(this);
            gbCharacteristic.evtIndicate().remove(this);

            // observe if notification is enabled.
            if (gbCharacteristicCCCD != null) {
                gbCharacteristicCCCD.evtRead().remove(this);
                gbCharacteristicCCCD.evtWritten().remove(this);
            }
        }
        if (bufferedPduSender != null) {
            bufferedPduSender = null;
            gbCharacteristic.evtWritten().remove(this);
            remoteDevice.evtMtuUpdated().remove(this);
        }
        remoteDevice.evtStateChanged().remove(this);
    }

    @Override
    public void onEvent(Object src, int evtType, Object evtData) {
        if (evtType == GBRemoteDevice.EVT_STATE_CHANGED) {
            int state = (int) evtData;

            Transceiver txrx = this.boundTransceiver;
            if (state == GBRemoteDevice.STATE_CONNECTED) {
                // reset RX buffer of bound transceiver
                if (txrx != null) {
                    txrx.reset();
                    // txrx.setReady(true);
                    txrx.setReady(gbCharacteristic.isNotifyEnabled() || gbCharacteristic.isIndicateEnabled());
                }

                // reset TX buffer
                BufferedPduSender sender = this.bufferedPduSender;
                if (sender != null) {
                    sender.setMaxSegmentSize(23 - 3);
                    sender.clear();
                }
            } else {
                if (txrx != null) {
                    txrx.setReady(false);
                }
            }
        }

        if (evtType == GBRemoteDevice.EVT_MTU_UPDATED) {
            bufferedPduSender.setMaxSegmentSize(((Integer) evtData) - 3);
        }

        switch (evtType) {
            case GBGattCharacteristic.EVT_NOTIFY:
            case GBGattCharacteristic.EVT_INDICATE: {
                byte[] pdu = (byte[]) evtData;
                if (src != this.gbCharacteristic) {
                    break;
                }
                // handle frame data
                Transceiver txrx = this.boundTransceiver;
                if (txrx != null && pdu != null) {
                    txrx.handleRcvData(pdu, 0, pdu.length);
                }
                break;
            }
            case GBGattCharacteristic.EVT_WRITTEN: {
                if (src != this.gbCharacteristic) {
                    break;
                }
                // send next segment
                BufferedPduSender sender = this.bufferedPduSender;
                if (sender != null) {
                    sender.nextSegment();
                }
                break;
            }
        }

        if (src == gbCharacteristicCCCD) {
            if (evtType == GBGattDescriptor.EVT_WRITTEN || evtType == GBGattDescriptor.EVT_READ) {

                Transceiver txrx = this.boundTransceiver;
                if (txrx != null) {
                    byte[] data = (byte[]) evtData;
                    if (data != null && data.length == 2 && data[1] == 0x00) {
                        txrx.setReady(data[0] != 0);
                    }
                }
            }
        }
    }

    @Override
    public boolean sendFrame(byte[] dat) {
        if (dat == null) {
            return false;
        }

        if (dat.length == 0) {
            return false;
        }

        GBRemoteDevice remoteDevice = gbCharacteristic.getService().getRemoteDevice();
        if (!remoteDevice.isConnected()) {
            return false;
        }
        //if (!remoteDevice.isDiscovered()) {
        //    return false;
        //} 是否发现服务并不一定影响写数据

        // use WRITE_NO_RESPONSE firstly.
        final int property = gbCharacteristic.getProperty();
        if ((property & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) {
            gbCharacteristic.writeByCommand(dat, false).startProcedure();
        } else {
            gbCharacteristic.writeByRequest(dat).startProcedure();
        }

        return true;
    }
}
