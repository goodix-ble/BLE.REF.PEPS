package com.goodix.ble.libuihelper.view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class AsyncBatchLayoutInflater extends LayoutInflater implements Runnable {
    public interface CB {
        void onLayoutInflaterReady(LayoutInflater inflater, ViewGroup container);
    }

    private Thread inflaterThread = null;
    private Handler inflaterHandler = new Handler(Looper.getMainLooper());

    // only for statistics
    private long ioInflateTime;
    private int ioInflateCnt;
    private long uiInflateTime;
    private int uiInflateCnt;

    private CB cb;
    private int[] layoutIds; // 先存储需要用加载的Layout，在io线程加载后，再复用来存储需要用ui线程加载的Layout
    private int layoutIdCnt = 0; // 记录需要加载的Layout有多少
    private int layoutIdRemainCnt = 0; // 记录需要用UI线程来加载的Layout有多少
    private int layoutIdRemainPos = 0; // 记录通过UI线程加载到第几个Layout了

    LayoutInflater orgInflater;
    ViewGroup root;
    SparseArray<ArrayList<View>> cache = new SparseArray<>(64);

    public AsyncBatchLayoutInflater add(@LayoutRes int layoutId) {
        return add(layoutId, 1);
    }

    public synchronized AsyncBatchLayoutInflater add(@LayoutRes int layoutId, int multiply) {
        for (int i = 0; i < multiply; i++) {
            // enlarge
            if (layoutIdCnt >= this.layoutIds.length) {
                int[] layoutIds = new int[this.layoutIds.length * 2];
                System.arraycopy(this.layoutIds, 0, layoutIds, 0, layoutIdCnt);
                this.layoutIds = layoutIds;
            }
            // add
            this.layoutIds[layoutIdCnt++] = layoutId;
        }
        return this;
    }

    public synchronized void start(CB cb) {
        this.cb = cb;
        inflaterThread = new Thread(this);
        inflaterThread.start();
    }

    public AsyncBatchLayoutInflater(LayoutInflater orgInflater, ViewGroup container, int initialCapacity) {
        super(orgInflater.getContext());
        this.orgInflater = orgInflater;
        this.root = container;
        this.layoutIds = new int[initialCapacity];
    }

    protected AsyncBatchLayoutInflater(Context context) {
        super(context);
    }

    @Override
    public LayoutInflater cloneInContext(Context newContext) {
        return new AsyncBatchLayoutInflater(newContext);
    }

    @Override
    public View inflate(int resource, @Nullable ViewGroup root, boolean attachToRoot) {
        ArrayList<View> views = cache.get(resource);
        if (views == null || views.isEmpty()) {
            return orgInflater.inflate(resource, root, attachToRoot);
        }
        View view = views.remove(views.size() - 1);
        if (attachToRoot && root != null) {
            root.addView(view);
            return root;
        }
        return view;
    }

    @Override
    public synchronized void run() {
        if (inflaterThread == Thread.currentThread()) {
            layoutIdRemainCnt = 0;
            layoutIdRemainPos = 0;
            ioInflateTime = System.currentTimeMillis();
            ioInflateCnt = 0;
            for (int i = 0; i < layoutIdCnt; i++) {
                int id = layoutIds[i];
                try {
                    View view = orgInflater.inflate(id, root, false);
                    ArrayList<View> views = cache.get(id);
                    if (views == null) {
                        views = new ArrayList<>();
                        cache.put(id, views);
                    }
                    views.add(view);
                    ioInflateCnt++;
                } catch (Throwable e) {
                    layoutIds[layoutIdRemainCnt++] = id; // 记录起来
                    //e.printStackTrace();
                }
            }
            inflaterThread = null;
            uiInflateTime = System.currentTimeMillis();
            uiInflateCnt = 0;
            ioInflateTime = uiInflateTime - ioInflateTime;
            inflaterHandler.post(this);
        } else {
            if (layoutIdRemainPos < layoutIdRemainCnt) {
                int id = layoutIds[layoutIdRemainPos++];
                try {
                    View view = orgInflater.inflate(id, root, false);
                    ArrayList<View> views = cache.get(id);
                    if (views == null) {
                        views = new ArrayList<>();
                        cache.put(id, views);
                    }
                    views.add(view);
                    uiInflateCnt++;
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                inflaterHandler.post(this);
            } else {
                uiInflateTime = System.currentTimeMillis() - uiInflateTime;
                if (cb != null) {
                    cb.onLayoutInflaterReady(this, root);
                    cb = null;
                }
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "inflate{ io: {time:" + ioInflateTime
                + ", cnt:" + ioInflateCnt
                + "},"
                + "ui: {time:" + uiInflateTime
                + ", cnt:" + uiInflateCnt
                + "}}";
    }
}
