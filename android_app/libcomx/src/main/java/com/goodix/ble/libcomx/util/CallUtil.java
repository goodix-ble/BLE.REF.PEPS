package com.goodix.ble.libcomx.util;

import java.io.IOException;

public class CallUtil {
    public static final String DEFAULT_SPLITTER = "<-";

    public static Appendable trace(int deep) {
        return trace(deep, null, null, 1); // skip current method. Not show current method.
    }

    public static Appendable trace(int deep, CharSequence splitter, Appendable out) {
        return trace(deep, splitter, out, 1); // skip current method. Not show current method.
    }

    public static Appendable trace(int deep, CharSequence splitter, Appendable out, int skip) {
        // [0] dalvik.system.VMStack.getThreadStackTrace(Native Method)
        // [1] java.lang.Thread.getStackTrace(Thread.java:1730)
        // [2] com.goodix.ble.libcomx.util.CallUtil.trace(CallUtil.java:27)
        // [3] com.goodix.ble.libcomx.util.CallUtil.trace(CallUtil.java:10)
        // [4] xxx(x.y:123) <- who call trace
        // [5] zzz(x.z:334) <- who call xxx(). we want to show this method.
        // An offset to skip this trace() at least.
        // skip [2] and [4]
        final int skipBias = 2; // 1 or 2, TBD

        // Thread.getStackTrace() will add 2 more elements.
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // skip native method which implements Thread.getStackTrace()
        int skipOffset = 0;
        for (StackTraceElement element : stackTrace) {
            skipOffset += 1;
            if (element.getClassName().equals(Thread.class.getName())) {
                break;
            }
        }

        return trace(stackTrace, deep, splitter, out, skip + skipBias + skipOffset);
    }

    public static Appendable trace(StackTraceElement[] stackTrace, int deep) {
        return trace(stackTrace, deep, DEFAULT_SPLITTER, null, 0);
    }

    public static Appendable trace(StackTraceElement[] stackTrace, int deep, CharSequence splitter, Appendable out, int skip) {
        if (deep < 1) {
            deep = 1;
        }
        if (out == null) {
            out = new StringBuffer(deep * 64);
        }
        if (splitter == null) {
            splitter = DEFAULT_SPLITTER;
        }

        // print call stack
        for (int i = skip, c = 0; i < stackTrace.length && c < deep; i++, c++) {
            StackTraceElement element = stackTrace[i];
            try {
                if (i > skip) {
                    out.append(splitter);
                }
                out.append(element.getMethodName())
                        .append("(")
                        .append(element.getFileName())
                        .append(":")
                        .append(String.valueOf(element.getLineNumber()))
                        .append(")");
            } catch (IOException ignored) {
                break;
            }
        }
        return out;
    }
}
