package com.goodix.ble.libuihelper.thread;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 在奔溃的时候，记录本进程的安卓日志到文件中，并且，如果可以的话，将当前Activity的截图保存下来。
 * 这些文件会保存在程序内部的 files 文件夹中，例如：/data/user/0/com.xx.yy/files/crash
 * 成功插入到异常处理链后，会输出一条日志说明已经插入成功，以及告知日志会输出到哪个目录
 * 如果，能够访问外部存储器，可以将日志输出到外部存储器
 * <p>
 * 注意：不允许使用这个类重复截获同一线程的崩溃异常
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CrashLogger implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashLogger";
    private static final String SUFFIX_LOG = ".log";
    private static final String SUFFIX_PNG = ".png";

    @Nullable
    private Thread.UncaughtExceptionHandler orgHandler;
    @Nullable
    private Activity currentActivity;

    private File baseDir;
    private File extDir; // 外部存储器的位置
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US);
    @Nullable
    private Date crashTime;

    public static boolean intercept(Application ctx) {
        return intercept(ctx, true);
    }

    public static boolean intercept(Application ctx, boolean useExternalStorage) {
        Thread thread = Thread.currentThread();

        CrashLogger crash = new CrashLogger();

        crash.orgHandler = thread.getUncaughtExceptionHandler();
        // 判断是否已经添加过了
        if (crash.orgHandler != null && crash.orgHandler.getClass().equals(crash.getClass())) {
            Log.e(TAG, "You have already registered this exception handler. Ignore this interception.");
            return false;
        }

        // 用于获得当前Activity，以便截图
        ctx.registerActivityLifecycleCallbacks(crash.lifecycleCallbacks);

        // 准备存储路径
        crash.baseDir = new File(ctx.getFilesDir(), "crash");
        if (!crash.baseDir.exists()) {
            if (!crash.baseDir.mkdir()) {
                Log.e(TAG, "Failed to create directory for crash: " + crash.baseDir.getAbsolutePath());
                return false;
            }
        }
        if (useExternalStorage) {
            crash.extDir = new File(ctx.getExternalFilesDir(null), "crash");
            if (!crash.extDir.exists()) {
                if (!crash.extDir.mkdir()) {
                    Log.w(TAG, "Failed to create directory on external storage for crash: " + crash.extDir.getAbsolutePath());
                    crash.extDir = null;
                }
            }
        }

        thread.setUncaughtExceptionHandler(crash);
        Log.i(TAG, "Uncaught exception handler is ready.");
        if (crash.extDir == null) {
            Log.i(TAG, "Dir on inner storage for crash log: " + crash.baseDir.getAbsolutePath());
        } else {
            Log.i(TAG, "Dir on external storage for crash log: " + crash.extDir.getAbsolutePath());
        }
        return true;
    }

    private CrashLogger() {
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        try {
            crashTime = new Date();
            Log.e(TAG, "------------------------- Crash -------------------------", e);
            saveAndroidLog();
            takeScreenshot();
            tryMoveFilesToExtStorage();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (orgHandler != null) {
                Log.e(TAG, "------------------ Other Crash Handler ------------------");
                orgHandler.uncaughtException(t, e);
            }
        }
    }

    private File getFile(File dir, String suffix) {
        if (crashTime == null) {
            crashTime = new Date();
        }
        return new File(dir, dateFormat.format(crashTime) + suffix);
    }

    private void saveAndroidLog() {
        String[] cmd = {"logcat", "-d", "-v", "threadtime", "-f", "logcat.log"};
        cmd[cmd.length - 1] = getFile(baseDir, SUFFIX_LOG).getAbsolutePath();
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.waitFor(10, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void takeScreenshot() {
        Activity activity = this.currentActivity;

        if (activity == null) return;

        View dView = activity.getWindow().getDecorView();
        dView.setDrawingCacheEnabled(true);
        dView.buildDrawingCache();

        FileOutputStream os = null;
        try {
            File png = getFile(baseDir, SUFFIX_PNG);
            boolean fileOk = true;
            if (!png.exists()) {
                fileOk = png.createNewFile();
            }
            if (fileOk) {
                os = new FileOutputStream(png);
                Bitmap bitmap = Bitmap.createBitmap(dView.getDrawingCache());
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, os);
                bitmap.recycle();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void tryMoveFilesToExtStorage() {
        if (extDir != null) {
            File innerLog = getFile(baseDir, SUFFIX_LOG);
            File innerPng = getFile(baseDir, SUFFIX_PNG);
            File extLog = getFile(extDir, SUFFIX_LOG);
            File extPng = getFile(extDir, SUFFIX_PNG);
            moveFile(innerLog, extLog);
            moveFile(innerPng, extPng);
        }
    }

    private void moveFile(File src, File dst) {
        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            is = new FileInputStream(src);
            os = new FileOutputStream(dst);
            byte[] buff = new byte[4 * 1024];
            while (is.read(buff) > 0) {
                os.write(buff);
            }
            is.close();
            is = null;
            os.close();
            os = null;
            //noinspection ResultOfMethodCallIgnored
            src.delete();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @SuppressWarnings("FieldCanBeLocal")
    private final Application.ActivityLifecycleCallbacks lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {

        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            currentActivity = activity;
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {

        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            if (currentActivity == activity) {
                currentActivity = null;
            }
        }
    };
}
