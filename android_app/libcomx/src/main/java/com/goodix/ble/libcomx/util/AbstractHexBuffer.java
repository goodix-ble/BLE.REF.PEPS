package com.goodix.ble.libcomx.util;

public abstract class AbstractHexBuffer {
    protected byte[] buffer;
    protected int pos;

    protected int posStart;
    protected int posEnd;

    protected boolean bigEndian = false; // little endian in default

    /**
     * 设置开始写入的数据的位置
     *
     * @param pos 表示指定范围{@link #setRange(int, int)}内的相对位置。从 0 开始。
     */
    public void setPos(int pos) {
        this.pos = this.posStart + pos;
        if (this.pos < this.posStart) this.pos = this.posStart;
        if (this.pos > this.posEnd) this.pos = this.posEnd;
    }

    public int getPos() {
        return this.pos - this.posStart;
    }

    public void setRange(int pos, int size) {
        if (this.pos < pos) this.pos = pos;

        this.posStart = pos;
        this.posEnd = pos + size;

        if (this.posEnd > this.buffer.length) {
            this.posEnd = this.buffer.length;
        }
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
        this.pos = 0;
        setRange(0, buffer.length);
    }

    /**
     * 获取剩余还未处理的字节数
     */
    public int getRemainSize() {
        return this.posEnd - this.pos;
    }

    public void setEndian(boolean bigEndian) {
        this.bigEndian = bigEndian;
    }
}
