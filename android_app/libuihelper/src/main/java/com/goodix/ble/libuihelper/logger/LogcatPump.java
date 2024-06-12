package com.goodix.ble.libuihelper.logger;

import android.util.Log;

import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.util.HexEndian;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LogcatPump {
    public static final int EVT_READ_LOG = 994;

    private Thread pumpThread = null;
    private Process adbProcess;
    private byte[] payload = new byte[5120];
    private Event<LogMsg> eventReadLog = new Event<>(this, EVT_READ_LOG);
    private byte[] readBuffer = new byte[8];
    private int logLevel = 0;
    LogMsg msg = new LogMsg(); // 减少内存分配

    public static class LogMsg {
        public int pid;
        public int tid;
        public long timestamp;
        public int lid;
        public int uid;
        public int level;
        public String tag;
        public String msg;
    }

    public Event<LogMsg> evtReadLog() {
        return eventReadLog;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public boolean start() {

        synchronized (LogcatPump.class) {
            if (pumpThread != null) {
                return false;
            }

            String[] cmd = {"logcat", "-v", "threadtime", "-B"};
            pumpThread = new Thread(() -> {
                // 有异常导致日志读取循环退出时，重新启动读取进程
                while (!pumpThread.isInterrupted()) {
                    try {
                        System.out.println("Start pump logcat.");
                        synchronized (LogcatPump.class) {
                            if (adbProcess != null) {
                                adbProcess.destroy();
                            }
                            adbProcess = Runtime.getRuntime().exec(cmd);
                        }

                        InputStream is = adbProcess.getInputStream();

                        while (!pumpThread.isInterrupted()) {
                            int len = readValue(is, 2);
                            int hdr = readValue(is, 2);

                            if (len == 0) break;

                            getBase(is, msg); // handled 16 bytes

                            if (hdr == (4 + 16 + 4 + 4)) {
                                // V4
                                msg.lid = readValue(is, 4);
                                msg.uid = readValue(is, 4);
                            } else if (hdr == (4 + 16 + 4)) {
                                // V2 V3
                                msg.lid = readValue(is, 4);
                            } else {
                                // drop unused bytes
                                for (int i = 16 + 4; i < hdr; i++) {
                                    //noinspection ResultOfMethodCallIgnored
                                    is.read();
                                }
                            }

                            getPayload(is, msg, len);

                            if (msg.level < logLevel) {
                                continue;
                            }
                            evtReadLog().postEvent(msg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // 输出异常原因
                    msg.level = Log.ERROR;
                    msg.tag = LogcatPump.class.getSimpleName();
                    try {
                        msg.msg = "LogcatPump exited. isInterrupted: " + pumpThread.isInterrupted() + ", exitValue: " + adbProcess.exitValue();
                    } catch (Exception e) {
                        msg.msg = "LogcatPump exited. isInterrupted: " + pumpThread.isInterrupted() + ", exitValue: " + e.getMessage();
                        e.printStackTrace();
                    }
                    System.out.println(msg.msg);
                    evtReadLog().postEvent(msg);
                }
                synchronized (LogcatPump.class) {
                    pumpThread = null;
                }
            });
            pumpThread.start();
        }
        return true;
    }

    public boolean stop() {
        synchronized (LogcatPump.class) {
            if (pumpThread != null) {
                adbProcess.destroy();
                pumpThread.interrupt();
                return true;
            }
        }
        return false;
    }

    private void getBase(InputStream di, LogMsg msg) throws IOException {
        msg.pid = readValue(di, 4);
        msg.tid = readValue(di, 4);
        msg.timestamp = (readValue(di, 4) * 1000L + readValue(di, 4) / 1000000L); // sec + nsec
        msg.lid = 0;
        msg.uid = 0;
    }

    private void getPayload(InputStream di, LogMsg msg, int payloadLen) throws IOException {
        int read = di.read(payload, 0, payloadLen);
        msg.level = payload[0];
        // get TAG
        final int posTagStart = 1;
        int posTagEnd = posTagStart;
        for (; posTagEnd < read; posTagEnd++) {
            if (payload[posTagEnd] == 0) {
                break;
            }
        }
        if (posTagEnd < read && posTagEnd > posTagStart) {
            msg.tag = new String(payload, posTagStart, posTagEnd - posTagStart, StandardCharsets.UTF_8);
        } else {
            msg.tag = "";
        }
        // get MSG
        final int posMsgStart = posTagEnd + 1;
        int posMsgEnd = read - 1;
        if (posMsgEnd > posMsgStart) {
            msg.msg = new String(payload, posMsgStart, posMsgEnd - posMsgStart, StandardCharsets.UTF_8);
        } else {
            msg.msg = "";
        }
    }

    private int readValue(InputStream is, int size) throws IOException {
        int read = is.read(readBuffer, 0, size);
        return HexEndian.fromByte(readBuffer, 0, read, false);
    }
}
