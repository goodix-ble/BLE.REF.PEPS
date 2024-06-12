package com.goodix.ble.libcomx.logger;

import com.goodix.ble.libcomx.ILogger;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class Logger {
    public static final SimpleDateFormat DATE_FORMAT_TIMESTAMP = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    public static final SimpleDateFormat DATE_FORMAT_LOG_FILE = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US);

    public static void v(final ILogger logger, String tag, String msg) {
        if (logger != null) {
            logger.v(tag, msg);
        }
    }


    public static void d(final ILogger logger, String tag, String msg) {
        if (logger != null) {
            logger.d(tag, msg);
        }
    }


    public static void i(final ILogger logger, String tag, String msg) {
        if (logger != null) {
            logger.i(tag, msg);
        }
    }


    public static void w(final ILogger logger, String tag, String msg) {
        if (logger != null) {
            logger.w(tag, msg);
        }
    }


    public static void e(final ILogger logger, String tag, String msg) {
        if (logger != null) {
            logger.e(tag, msg);
        }
    }


    public static void e(final ILogger logger, String tag, String msg, Throwable e) {
        if (logger != null) {
            logger.e(tag, msg, e);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Formatter based API
    // If logger is null, the string formatting won't work. So that, we can
    // save some CPU consumptions.
    ///////////////////////////////////////////////////////////////////////////

    public static void v(final ILogger logger, String tag, String msgFormat, Object... args) {
        if (logger != null) {
            logger.v(tag, String.format(msgFormat, args));
        }
    }


    public static void d(final ILogger logger, String tag, String msgFormat, Object... args) {
        if (logger != null) {
            logger.d(tag, String.format(msgFormat, args));
        }
    }


    public static void i(final ILogger logger, String tag, String msgFormat, Object... args) {
        if (logger != null) {
            logger.i(tag, String.format(msgFormat, args));
        }
    }


    public static void w(final ILogger logger, String tag, String msgFormat, Object... args) {
        if (logger != null) {
            logger.w(tag, String.format(msgFormat, args));
        }
    }


    public static void e(final ILogger logger, String tag, String msgFormat, Object... args) {
        if (logger != null) {
            logger.e(tag, String.format(msgFormat, args));
        }
    }


    public static void e(final ILogger logger, String tag, String msgFormat, Throwable e, Object... args) {
        if (logger != null) {
            logger.e(tag, String.format(msgFormat, args), e);
        }
    }
}
