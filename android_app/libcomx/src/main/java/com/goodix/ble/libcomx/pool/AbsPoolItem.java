package com.goodix.ble.libcomx.pool;

public class AbsPoolItem implements IRecyclable {
    Pool host;
    int refCnt = 0;

    @Override
    public void reuse(Pool pool) {
        host = pool;
        refCnt = 0;
    }

    @Override
    public int getRefCnt() {
        return refCnt;
    }

    @Override
    public synchronized void retain() {
        refCnt++;
    }

    @Override
    public synchronized void release() {
        refCnt--;
        if (refCnt < 1) {
            host.recycle(this);
        }
    }
}
