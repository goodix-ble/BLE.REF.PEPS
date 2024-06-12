package com.goodix.ble.libcomx.util;

@SuppressWarnings("unused")
public class HexBuilder extends AbstractHexBuffer {

    public HexBuilder(int size) {
        if (size < 0) size = 0;
        setBuffer(new byte[size]);
    }

    public HexBuilder put(int val, int size) {
        return put(val, size, bigEndian);
    }

    public HexBuilder put(int val, int size, boolean bigEndian) {
        if (pos + size <= this.posEnd) {
            HexEndian.toByte(val, buffer, pos, size, bigEndian);
            pos += size;
        } else {
            throw new IllegalStateException("buffer is to small. pos = [" + pos + "], size = [" + size + "]");
        }
        return this;
    }

    public HexBuilder putLong(long val, int size, boolean bigEndian) {
        if (pos + size <= this.posEnd) {
            HexEndian.toByteLong(val, buffer, pos, size, bigEndian);
            pos += size;
        } else {
            throw new IllegalStateException("buffer is to small. pos = [" + pos + "], size = [" + size + "]");
        }
        return this;
    }

    public HexBuilder put(byte[] dat) {
        if (dat != null) {
            return put(dat, 0, dat.length);
        }
        return this;
    }

    public HexBuilder put(byte[] dat, int pos, int size) {
        if (dat != null) {
            if (this.pos + size <= this.posEnd) {
                for (int i = 0; i < size; i++) {
                    buffer[this.pos] = dat[pos];
                    this.pos++;
                    pos++;
                }
            } else {
                throw new IllegalStateException("buffer is to small. pos = [" + pos + "], size = [" + size + "]");
            }
        }
        return this;
    }

    public int peek(int pos) {
        pos = pos + posStart;
        if (pos < this.posEnd) {
            return buffer[pos] & 0xFF;
        }
        return 0;
    }
}
