package com.goodix.ble.libcomx.pool;

import java.util.ArrayList;

public class Pool {
    private Class<? extends IRecyclable> itemClass;
    private ArrayList<IRecyclable> pool;
    private int maxInstanceCount = 0;

    public Pool(Class<? extends IRecyclable> itemClass, int capabilities) {
        this.itemClass = itemClass;
        pool = new ArrayList<>(capabilities);
    }

    public synchronized <T> T get() {
        IRecyclable item = null;
        if (pool.isEmpty()) {
            try {
                item = itemClass.newInstance();
                item.reuse(this);
                maxInstanceCount++;
            } catch (InstantiationException | IllegalAccessException ignored) {
            }
        } else {
            // 对象池中的对象有可能因为线程安全问题，还处于被引用状态，所以暂时跳过
            int i = pool.size() - 1;
            for (; i > 0; i--) {
                if (pool.get(i).getRefCnt() < 1) {
                    break;
                }
            }
            // 一定会重置最底部的对象的引用计数
            item = pool.remove(i);
            item.reuse(this);
        }
        //noinspection unchecked
        return (T) item;
    }

    synchronized void recycle(IRecyclable item) {
        pool.add(item);
    }
}
