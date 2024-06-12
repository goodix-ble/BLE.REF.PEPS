package com.goodix.ble.libuihelper.logger;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class LogcatUtil {
    private static final String TAG = "DumpLog";
    public static final SimpleDateFormat LOG_FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US);
    private static Process recordProcess = null;
    private static boolean recordRunning = false;

    public static String createFileName() {
        return LOG_FILE_DATE_FORMAT.format(new Date());
    }

    public static File saveAndroidLog(Context ctx) {
        File logFile = new File(ctx.getExternalFilesDir(null), "logcat/" + createFileName() + ".log");
        return saveAndroidLog(logFile);
    }

    public static File saveAndroidLog(File logFile) {
        File dir = logFile.getParentFile();
        if (dir != null) {
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.e(TAG, "Failed to create directory on external storage for log: " + logFile.getAbsolutePath());
                }
            }
        }

        String[] cmd = {"logcat", "-d", "-v", "threadtime", "-f", "logcat.log"};
        cmd[cmd.length - 1] = logFile.getAbsolutePath();
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.waitFor(10, TimeUnit.SECONDS);
            }
            return logFile;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean recordAndroidLog(Context ctx, int sizePerFileKb, int rotateCount) {
        File logFile = new File(ctx.getExternalFilesDir(null), "logcat/record/" + createFileName() + ".log");
        return recordAndroidLog(logFile, sizePerFileKb, rotateCount);
    }

    public static boolean recordAndroidLog(File logFile, int sizePerFileKb, int rotateCount) {
        if (logFile == null || sizePerFileKb <= 0 || rotateCount <= 0) {
            return false;
        }

        synchronized (LogcatUtil.class) {
            if (recordProcess != null) {
                return false;
            }

            File dir = logFile.getParentFile();
            if (dir != null) {
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        Log.e(TAG, "Failed to create directory on external storage for log: " + logFile.getAbsolutePath());
                        return false;
                    }
                }
            }

            final String[] cmd = {
                    "logcat"
                    , "-v", "threadtime"
                    , "-r", String.valueOf(sizePerFileKb)
                    , "-n", String.valueOf(rotateCount)
                    , "-f", logFile.getAbsolutePath()};

            recordRunning = true;
            new Thread(() -> {
                // 不断的启动读取进程
                while (recordRunning) {
                    System.out.println("Start recording logcat.");
                    try {
                        synchronized (LogcatUtil.class) {
                            recordProcess = Runtime.getRuntime().exec(cmd);
                        }
                        recordProcess.waitFor();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                synchronized (LogcatUtil.class) {
                    recordProcess = null;
                }
            }, LogcatUtil.class.getSimpleName()).start();
        }
        return true;
    }

    public static boolean stopRecord() {
        synchronized (LogcatUtil.class) {
            recordRunning = false;
            if (recordProcess != null) {
                recordProcess.destroy();
                return true;
            }
        }
        return false;
    }
}
