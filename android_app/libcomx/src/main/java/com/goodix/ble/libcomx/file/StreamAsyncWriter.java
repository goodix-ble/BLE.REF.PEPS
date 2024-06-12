package com.goodix.ble.libcomx.file;

import com.goodix.ble.libcomx.event.Event;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 自动使用IO线程异步写入数据到文件中
 * 同步队列用于缓存要写入的字节
 * 保活IO线程一段时间后，如果没有数据写入，就自动关闭文件并结束线程
 * <p>
 * 线程安全：
 * doWork：获取输出流、【判断输出流，进入空闲态或运行态】、等待输出全部数据、【进入结束态】、关闭输出流、判断是否需要调用write()、【判断启动线程并进入准备态】、写入数据
 * write：【判断启动线程并进入准备态】、写入数据
 * |        |  write  |  doWork
 * |--------|---------|-----------
 * | write  |    V    |    W1
 * | doWork |   W1    |    V
 * W1：在“判断是否需要调用write()”中插入数据，有概率导致数据丢失。但因为延时的存在，插入数据不应该会落在这个同步块之内
 */
@SuppressWarnings("WeakerAccess")
public abstract class StreamAsyncWriter implements IStreamWriter {
    public static final int EVT_ERROR = 206;

    private static final int STATE_IDLE = 0;
    private static final int STATE_STARTING = 1;
    private static final int STATE_RUNNING = 2;
    private static final int STATE_STOPPING = 3;

    private final String threadName = getClass().getName() + "-WriterThread";
    private Thread currentThread = null;
    private int threadState = STATE_IDLE;

    private ArrayBlockingQueue<byte[]> dataList = new ArrayBlockingQueue<>(1024);

    private int intervalMillis = 10;
    private int ttlMillis = 1000;

    private boolean flushEveryWrite = false;

    private boolean disposeFlag = false;

    Event<Throwable> errorEvent = new Event<>(this, EVT_ERROR);

    public Event<Throwable> evtError() {
        return errorEvent;
    }

    public void setImmediatelyFlush(boolean enable) {
        this.flushEveryWrite = enable;
    }

    public void setTTL(int millis) {
        this.ttlMillis = millis;
    }

    /**
     * 写入线程
     */
    void doWrite() {
        OutputStream outputStream = null;

        // 得到输出流
        try {
            outputStream = onPrepareOutputStream();
        } catch (Exception e) {
            e.printStackTrace();
            errorEvent.postEvent(e);
        }

        // 进入运行状态
        synchronized (this) {
            if (outputStream == null) {
                threadState = STATE_IDLE;
                return;
            }
            threadState = STATE_RUNNING;
            currentThread = Thread.currentThread();
        }

        // 保活，并输出缓存中的数据
        for (int ttl = 0; ; ) {
            try {
                // 开始写入前都被关闭了，就丢掉需要写入的数据
                if (disposeFlag) {
                    break;
                }

                if (onWriteAll(outputStream, flushEveryWrite)) {
                    ttl = 0;
                }

                if (ttl > ttlMillis) {
                    break;
                }

                // 写入文件后，需要等待一小段时间再写入，避免频繁写文件
                // 有可能在这里等待的时候，被关闭了，直接走interrupted分支
                Thread.sleep(intervalMillis);
                ttl += intervalMillis;
            } catch (Exception e) {
                e.printStackTrace();
                errorEvent.postEvent(e);
                // 发生错误了就停止此次的输出循环
                // 并且丢弃缓存中的数据
                dataList.clear();
                break;
            }
        }

        // 进入结束状态
        synchronized (this) {
            threadState = STATE_STOPPING;
        }

        // 关闭输出流
        try {
            onCloseOutputStream(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
            errorEvent.postEvent(e);
        }

        try {
            outputStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            errorEvent.postEvent(ex);
        }

        // 当前线程完成停止后，如果任然处于停止态且还有数据未发送，就立即拉起一个新的线程来准备写入数据
        if (!dataList.isEmpty()) {
            write(null);
        }
    }

    protected abstract OutputStream onPrepareOutputStream() throws Exception;

    protected abstract void onCloseOutputStream(OutputStream outputStream) throws Exception;

    protected boolean onWriteAll(OutputStream outputStream, boolean flushEveryWrite) throws IOException {
        boolean hasOutput = false;
        if (outputStream != null) {
            final ArrayBlockingQueue<byte[]> dataPool = this.dataList;
            // 写入缓存中全部剩余的内容
            while (true) {
                byte[] take = dataPool.poll();
                if (take == null) break;
                hasOutput = true;
                outputStream.write(take);
            }

            if (flushEveryWrite) {
                outputStream.flush();
            }
        }
        return hasOutput;
    }

    public boolean write(byte[] dat) {
        /*
         * 需要确保：
         * 1、需要启动一个新的线程时，旧的线程已经结束了
         * 2、不需要启动一个新的线程时，旧的线程不能处于关闭阶段
         *
         * 线程分为4个状态：空闲、准备、运行、清理
         * 1、当线程处于准备和运行状态时，不需要启动新的线程
         * 2、当线程处于空闲或结束阶段时，需要启动新线程
         * 3、新线程需要等待旧线程执行结束后才开始。待定
         */
        synchronized (this) {
            if (disposeFlag) {
                return false;
            }

            if (threadState == STATE_STOPPING || threadState == STATE_IDLE) {
                threadState = STATE_STARTING;
                new Thread(this::doWrite, threadName).start();
            }
        }


        // 可以通过写入0字节数据来预先启动写入线程
        if (dat == null || dat.length == 0) {
            return true;
        }

        try {
            // 线程启动后，会等待TTL到期，所以，不用担心线程运行会先于塞数据
            // 当队列满的时候，卡住
            int timeout = ttlMillis | (intervalMillis << 1); // 求一个相对比较大的等待时间，简化计算方式
            dataList.offer(dat, timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            errorEvent.postEvent(e);
        }
        return true;
    }

    /**
     * 中断阻塞，关闭线程，关闭文件
     */
    public void close() {
        disposeFlag = true;

        // 终止内部线程的等待状态
        final Thread currentThread = this.currentThread;
        if (currentThread != null && currentThread.isAlive()) {
            currentThread.interrupt();
        }
    }
}
