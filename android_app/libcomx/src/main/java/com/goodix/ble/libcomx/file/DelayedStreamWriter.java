package com.goodix.ble.libcomx.file;

import com.goodix.ble.libcomx.event.Event;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 异步间隔写入
 * 同步队列用于缓存要写入的字节
 * 也可使用字节数据来支持数据拷贝模式（后续实现）
 */
@SuppressWarnings("WeakerAccess")
public class DelayedStreamWriter implements IStreamWriter {
    public static final int EVT_ERROR = 206;
    Thread writerThread = new Thread(this::doWrite);
    ArrayBlockingQueue<byte[]> dataList = new ArrayBlockingQueue<>(1024);

    protected OutputStream outputStream;
    int intervalMillis = 100;
    boolean flushEveryWrite = false;

    boolean startedFlag = false; // 线程只能启动一次。标记一下，在需要写入的时候启动一次。
    boolean closeFlag = false;

    Event<Throwable> errorEvent = new Event<>(this, EVT_ERROR);

    public Event<Throwable> evtError() {
        return errorEvent;
    }

    public void setImmediatelyFlush(boolean enable) {
        this.flushEveryWrite = enable;
    }

    public DelayedStreamWriter() {
    }

    public DelayedStreamWriter(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * 写入线程
     */
    void doWrite() {
        // 开始
        try {
            onStartThread();
        } catch (Exception e) {
            e.printStackTrace();
            errorEvent.postEvent(e);
        }

        // 持续记录
        for (; ; ) {
            try {
                // 开始写入前都被关闭了，就丢掉需要写入的数据
                if (closeFlag) {
                    outputStream.close();
                    break;
                }

                onWrite(flushEveryWrite);

                // 写入文件后，需要等待一小段时间再写入，避免频繁写文件
                // 有可能在这里等待的时候，被关闭了，直接走interrupted分支
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                break;
            } catch (IOException e) {
                e.printStackTrace();
                errorEvent.postEvent(e);
            }
        }

        // 结束
        try {
            onStopThread();

            if (closeFlag) {
                OutputStream outputStream = this.outputStream;
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorEvent.postEvent(e);
        }
    }

    protected void onStartThread() throws Exception {
        // e.g. prepare output stream
    }

    protected void onStopThread() throws Exception {
        // e.g. release resource
    }

    protected void onWrite(boolean flushEveryWrite) throws InterruptedException, IOException {
        // 有可能在这里等待的时候，被关闭了，直接走interrupted分支
        byte[] take = dataList.take();

        OutputStream outputStream = this.outputStream;
        if (outputStream != null) {

            // 写入缓存中全部剩余的内容
            do {
                outputStream.write(take);
                take = dataList.poll(); // 预取
            } while (take != null);

            if (flushEveryWrite) {
                outputStream.flush();
            }
        }
    }

    @Override
    public boolean write(byte[] dat) {
        if (dat == null) {
            return false;
        }

        if (!startedFlag) {
            synchronized (this) {
                if (!startedFlag) {
                    startedFlag = true;
                    writerThread.start();
                }
            }
        }

        // 可以通过写入0字节数据来预先启动写入线程
        if (dat.length == 0) {
            return true;
        }

        try {
            dataList.put(dat);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 中断阻塞，关闭线程，关闭文件
     */
    @Override
    public void close() {
        closeFlag = true;
        writerThread.interrupt();
        //
    }
}
