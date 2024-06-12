package com.goodix.ble.libcomx.util;

import java.nio.charset.Charset;

@SuppressWarnings("unused")
public class HexReader extends AbstractHexBuffer {

    public HexReader(byte[] pdu) {
        setBuffer(pdu);
    }

    public int get(int size) {
        return get(size, bigEndian);
    }

    public int get(int size, boolean bigEndian) {
        if (this.pos + size > this.posEnd) {
            return 0;
        } else {
            int val = HexEndian.fromByte(buffer, pos, size, bigEndian);
            pos += size;
            return val;
        }
    }

    public long getLong(int size, boolean bigEndian) {
        if (this.pos + size > this.posEnd) {
            return 0;
        } else {
            long val = HexEndian.fromByteLong(buffer, pos, size, bigEndian);
            pos += size;
            return val;
        }
    }

    public void get(byte[] out, int startPosInOut, int size) {
        if (out != null) {
            for (int i = 0; i < size && this.pos < buffer.length && startPosInOut < out.length; i++) {
                out[startPosInOut] = buffer[this.pos];
                this.pos++;
                startPosInOut++;
            }
        }
    }

    public void get(byte[] out) {
        if (out != null) {
            get(out, 0, out.length);
        }
    }

    public String getString(Charset charset, int size) {
        if (this.pos + size > this.posEnd) {
            size = this.posEnd - this.pos;
        }

        String s = new String(this.buffer, this.pos, size, charset);
        this.pos += size;

        return s;
    }
}
