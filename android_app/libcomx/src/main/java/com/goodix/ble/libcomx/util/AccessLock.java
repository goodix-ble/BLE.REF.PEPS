package com.goodix.ble.libcomx.util;

import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.event.IEventListener;

import java.util.ArrayList;

public class AccessLock {
    public static final int EVT_LOCKED = 970;
    public static final int EVT_LOCK_ACQUIRED = 355;

    public interface CB {
        void onLockAcquired(AccessLock lock);
    }

    /**
     * Notify the status of lock
     * The UI layer can use this status to enable widgets.
     * The caller can use this event to try acquire the lock.
     * null: the lock is idle.
     * nonnull: the lock is acquired.
     */
    private Event<Object> eventLocked = new Event<>(this, EVT_LOCKED);

    private ArrayList<Object> unlockQueue = new ArrayList<>();

    private Object owner;

    public boolean acquireLock(final CB requester) {
        return innerAcquireLock(requester);
    }

    public boolean acquireLock(final IEventListener<Void> requester) {
        return innerAcquireLock(requester);
    }

    private boolean innerAcquireLock(final Object requester) {
        boolean result = false;

        if (requester != null) {
            synchronized (this) {
                if (owner != null) {
                    // wait for lock
                    unlockQueue.add(requester);
                } else {
                    owner = requester;
                    result = true;
                }
            }
        }

        if (result) {
            // acquire directly
            if (requester instanceof CB) {
                ((CB) requester).onLockAcquired(this);
            } else if (requester instanceof IEventListener) {
                //noinspection unchecked
                ((IEventListener<Void>) requester).onEvent(this, EVT_LOCK_ACQUIRED, null);
            }
            // notify
            eventLocked.postEvent(requester);
        }
        return result;
    }

    public synchronized void releaseLock(final Object requester) {
        boolean idle = false;
        Object nextOwner = null;

        if (requester != null) {
            synchronized (this) {
                // if current owner, it is not in the queue.
                if (this.owner == requester) {
                    this.owner = null;
                    idle = true;

                    // lock again?
                    if (!unlockQueue.isEmpty()) {
                        nextOwner = unlockQueue.remove(0);
                        this.owner = nextOwner;
                        idle = false;
                    }
                } else {
                    // just remove the listener.
                    unlockQueue.remove(requester);
                }
            }
        }

        if (nextOwner != null) {
            if (nextOwner instanceof CB) {
                ((CB) nextOwner).onLockAcquired(this);
            } else if (nextOwner instanceof IEventListener) {
                //noinspection unchecked
                ((IEventListener<Void>) nextOwner).onEvent(this, EVT_LOCK_ACQUIRED, null);
            }
            eventLocked.postEvent(nextOwner);
        }
        if (idle) {
            eventLocked.postEvent(null);
        }
    }

    public synchronized boolean isLocked() {
        return owner != null;
    }

    public Event<Object> evtLocked() {
        return eventLocked;
    }

    public <T> T getOwner() {
        //noinspection unchecked
        return (T) owner;
    }

    public ArrayList<Object> getPendingList(ArrayList<Object> out) {
        if (out == null) {
            out = new ArrayList<>();
        }
        synchronized (this) {
            out.addAll(unlockQueue);
        }
        return out;
    }
}
