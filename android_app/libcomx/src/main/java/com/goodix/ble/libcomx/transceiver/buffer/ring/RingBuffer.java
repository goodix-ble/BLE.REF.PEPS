package com.goodix.ble.libcomx.transceiver.buffer.ring;

/**
 * 因为只在解码线程中使用，所以，可以不考虑线程安全
 * case 1:
 * D++++
 * 00000  len=w-r=0-0=0  free=r-w=0-0=0??
 * case 2:
 * r+w++
 * VV000  len=w-r=2-0=2  free=r-w=0-2=-2
 * case 3:
 * r+++w
 * VVVV0  len=w-r=4-0=4  free=r-w=0-4=-4
 * case 4:
 * wr+++
 * 0VVVV  len=w-r=(5)+(0-1)=4  free=r-w=1-0=1
 * case 5:
 * w+++r
 * 0000V  len=w-r=(5)+(0-4)=1  free=r-w=4-0=4
 * case 6:
 * ++++D
 * 00000  len=w-r=4-4=0  free=r-w=4-4=0
 */
public class RingBuffer implements IRingBufferMutable {
    private byte[] buffer;
    private int writePos;
    private int readPos;

    public RingBuffer(int size) {
        this.buffer = new byte[size];
    }

    @Override
    public int getSize() {
        int dataSize = writePos - readPos;
        if (dataSize < 0) dataSize = buffer.length + dataSize;
        return dataSize;
    }

    @Override
    public int getFreeSize() {
        int freeSize = buffer.length - getSize() - 1;
        return freeSize < 0 ? 0 : freeSize;
    }

    @Override
    public int getWritePos() {
        return writePos;
    }

    @Override
    public int getReadPos() {
        return readPos;
    }

    @Override
    public int put(byte[] dat, int pos, int size) {
        if (dat == null) return 0;

        int spaceSize = buffer.length - getSize();

        // 至少要保留1个字节的空位
        spaceSize -= 1; //
        if (spaceSize <= 0) return 0;

        if (size <= 0) size = dat.length;

        if (size > spaceSize) size = spaceSize;

        for (int i = 0; i < size; i++) {
            put(dat[pos++]);
        }

        return size;
    }

    @Override
    public boolean put(byte dat) {
        int nextPos = writePos + 1;

        if (nextPos >= buffer.length) {
            nextPos -= buffer.length;
        }

        if (nextPos != readPos) {
            buffer[writePos] = dat;
            writePos = nextPos;
            return true;
        }
        return false;
    }

    @Override
    public int get(int pos) {
        // 转换为绝对地址
        pos = pos + readPos;
        // 溢出
        if (pos >= buffer.length) {
            pos = pos % buffer.length;
        }
        return 0xFF & buffer[pos];
    }

    @Override
    public int get(int pos, byte[] out) {
        if (out == null) return 0;

        int size = getSize();

        if (size <= 0) return 0;

        if (pos > size) {
            pos = pos % size;
        }
        pos = pos + readPos;


        if (size > out.length) size = out.length;

        for (int i = 0; i < size; i++) {
            int p = pos + i;
            if (p >= buffer.length) {
                p -= buffer.length;
            }
            out[i] = buffer[p];
        }

        return size;
    }

    @Override
    public int getIntValue(int pos, int length, boolean bigEndian) {
        int size = getSize();

        if (size <= 0) return 0;

        if (pos > size) {
            pos = pos % size;
        }
        pos = pos + readPos;


        if (length > 4) length = 4;
        if (size > length) size = length;

        int val = 0;
        // 先按大端方式拼装
        for (int i = 0; i < size; i++) {
            int p = pos + i;
            if (p >= buffer.length) {
                p -= buffer.length;
            }
            val <<= 8;
            val |= (0xFF & buffer[p]);
        }

        // 如果需要再转为小端
        if (!bigEndian) {
            int tmp = val;
            val = 0;
            for (int i = 0; i < size; i++) {
                val <<= 8;
                val |= (0xFF & tmp);
                tmp >>= 8;
            }
        }

        return val;
    }

    @Override
    public int pop(int pos, byte[] out) {
        int v = get(pos, out);
        drop(pos + v); // 指定位置的数据及其之前的数据都要被移除掉
        return v;
    }

    @Override
    public void drop(int size) {
        int dataSize = writePos - readPos;
        if (dataSize < 0) {
            dataSize = buffer.length + dataSize;
            if (size < dataSize) {
                readPos = (readPos + size) % buffer.length;
            } else {
                readPos = writePos;
            }
        } else {
            if (size < dataSize) {
                readPos += size;
            } else {
                readPos = writePos;
            }
        }
    }

    @Override
    public void dropAll() {
        readPos = writePos;
    }
}
