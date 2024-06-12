package com.goodix.ble.libcomx.event;

import com.goodix.ble.libcomx.pool.IRecyclable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * Every src shall define event types for itself.
 * Every event type specifies the type of event data.
 * User use src to judge where the event comes from.
 * <p>
 * Memory pool is in use:
 * If the dat is an instance of {@link IRecyclable},
 * {@link Dispatcher} will call {@link IRecyclable#retain()} ()} before dispatching,
 * {@link Dispatcher} will call {@link IRecyclable#release()} after dispatching.
 *
 * @param <T> Specify the type of event data. It is used to note the type of event data.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Event<T> implements IEventListener<T> {
    private int dispatcherPoolMaxItem = 16;
    private CopyOnWriteArrayList<IEventListener> callbackList = new CopyOnWriteArrayList<>();
    private ArrayList<Dispatcher> dispatcherPool = new ArrayList<>(dispatcherPoolMaxItem);
    private Executor executor;
    private Event parent;
    private Object tag;
    private boolean isOneshot;
    private boolean isShotted;
    private Object defaultSrc;
    private int defaultEvtType;

    public Event() {
    }

    public Event(Object src, int evtType) {
        this.defaultSrc = src;
        this.defaultEvtType = evtType;
    }

    /**
     * register a generic listener to the event.
     */
    public synchronized Event<T> register(IEventListener callback) {
        callbackList.addIfAbsent(callback);
        return this;
    }

    /**
     * specify a template parameter.
     */
    public synchronized Event<T> register2(IEventListener<T> callback) {
        callbackList.addIfAbsent(callback);
        return this;
    }

    public synchronized Event<T> remove(IEventListener callback) {
        boolean needRemove = callbackList.size() > 0;
        callbackList.remove(callback);
        if (needRemove && parent != null && callbackList.size() == 0) {
            parent.remove(this);
            parent = null; // 释放对父类的引用
        }
        return this;
    }

    public Event<T> clear() {
        return clear(null);
    }

    public synchronized Event<T> clear(Object tag) {
        // 不管自身是否满足条件，遍历子事件，找到符合条件的
        // 递归让所有的 sub scope 监听器都释放监听
        if (tag != null) {
            for (IEventListener listener : callbackList) {
                if (listener instanceof Event) {
                    ((Event) listener).clear(tag);
                }
            }
        }

        // 判断自身，如果满足条件，从父事件中移除
        if (tag == null || this.tag == tag) {
            callbackList.clear();
            if (parent != null) {
                parent.remove(this);
                parent = null; // 释放对父类的引用
            }
            this.tag = null; // 释放对tag的引用，因为不知道tag引用的什么，最好释放掉
        }
        return this;
    }

    public Event<T> subEvent() {
        return subEvent(null, false);
    }

    public Event<T> subEvent(Object tag) {
        return subEvent(tag, false);
    }

    public synchronized Event<T> subEvent(Object tag, boolean oneshot) {
        Event<T> mgr = new Event<>();
        mgr.tag = tag;
        mgr.parent = this;
        mgr.isOneshot = oneshot;
        mgr.isShotted = false;
        callbackList.addIfAbsent(mgr);
        return mgr;
    }

    public Event<T> setExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public Event<T> setDisposer(EventDisposer cleaner) {
        if (cleaner != null) {
            cleaner.add(this);
        }
        return this;
    }

    public void postEvent(final T dat) {
        if (defaultSrc == null) {
            throw new IllegalStateException("Please specify event src.");
        }
        onEvent(defaultSrc, defaultEvtType, dat);
    }

    public void postEvent(Object src, int type, final T dat) {
        onEvent(src, type, dat);
    }

    /**
     * @deprecated please refer to {@link Event#postEvent}
     */
    @Deprecated
    @Override
    public void onEvent(final Object src, final int type, final Object dat) {
        // 防止临界情况下，二次进入
        if (isOneshot) {
            if (isShotted) {
                return;
            } else {
                // 减少锁的使用
                synchronized (this) {
                    if (isShotted) return;
                    isShotted = true;
                }
            }
        }

        Executor executor = this.executor;
        if (executor == null) {
            boolean isRecyclable = dat instanceof IRecyclable;
            if (isRecyclable) {
                ((IRecyclable) dat).retain();
            }
            for (IEventListener callback : callbackList) {
                try {
                    //noinspection unchecked
                    callback.onEvent(src, type, dat);
                } catch (Exception e) {
                    throw new RuntimeException("Error in dispatch event to " + callback.getClass().getName() + ": " + e.getMessage(), e);
                }
            }
            if (isOneshot) {
                clear();
            }
            if (isRecyclable) {
                ((IRecyclable) dat).release();
            }
            return;
        }

        // there is an issue when post execution and clear list immediately,
        // so we copy callback list before dispatching.
        executor.execute(allocDispatcher(src, type, dat, callbackList));
    }

    private synchronized Dispatcher allocDispatcher(Object src, int type, Object dat
            , Collection<? extends IEventListener> collection) {
        Dispatcher dispatcher;
        if (dispatcherPool.isEmpty()) {
            dispatcher = new Dispatcher(src, type, dat, collection);
        } else {
            dispatcher = dispatcherPool.remove(dispatcherPool.size() - 1);
            dispatcher.src = src;
            dispatcher.type = type;
            dispatcher.dat = dat;
            dispatcher.addAll(collection);
        }
        if (dat instanceof IRecyclable) {
            ((IRecyclable) dat).retain();
        }
        return dispatcher;
    }

    private synchronized void freeDispatcher(Dispatcher dispatcher) {
        dispatcher.clear();
        if (dispatcherPool.size() < dispatcherPoolMaxItem) {
            dispatcherPool.add(dispatcher);
        }
    }

    private class Dispatcher extends CopyOnWriteArrayList<IEventListener>
            implements Runnable {

        Object src;
        int type;
        Object dat;

        Dispatcher(Object src, int type, Object dat
                , Collection<? extends IEventListener> collection) {
            super(collection);
            this.src = src;
            this.type = type;
            this.dat = dat;
        }

        @Override
        public void run() {
            for (IEventListener callback : this) {
                try {
                    //noinspection unchecked
                    callback.onEvent(src, type, dat);
                } catch (Exception e) {
                    throw new RuntimeException("Error in event listener: " + callback.getClass().getName() + ": " + e.getMessage(), e);
                }
            }
            if (dat instanceof IRecyclable) {
                ((IRecyclable) dat).release();
            }
            freeDispatcher(this);
            if (isOneshot) {
                clear();
            }
        }
    }
}
