package com.goodix.ble.libcomx.logger;

import com.goodix.ble.libcomx.ILogger;

import java.util.HashMap;

/**
 * 用于给Tag增加前缀，达到按层级输出日志的效果
 */
public class PrefixedLogger implements ILogger {
    private ILogger output;
    private String tagPrefix = null;
    private String tagDelimiter = "-";
    private HashMap<String, String> tagCache = new HashMap<>(32);

    public PrefixedLogger(ILogger output) {
        this.output = output;
    }

    public PrefixedLogger(ILogger output, String tagPrefix) {
        this.output = output;
        this.tagPrefix = tagPrefix;
    }

    public void setTagPrefix(String tagPrefix) {
        this.tagPrefix = tagPrefix;
    }

    public void setTagDelimiter(String tagDelimiter) {
        this.tagDelimiter = tagDelimiter;
    }

    public synchronized void clearTagCache() {
        tagCache.clear();
    }

    private synchronized String getTag(String tag) {
        String prefix = this.tagPrefix;

        if (tag == null) {
            return prefix != null ? prefix : "null";
        }

        if (prefix == null) {
            return tag;
        }

        String finalTag = tagCache.get(tag);
        if (finalTag == null) {
            finalTag = prefix + tagDelimiter + tag;
            tagCache.put(tag, finalTag);
        }
        return finalTag;
    }

    @Override
    public void v(String tag, String msg) {
        if (output != null) {
            output.v(getTag(tag), msg);
        }
    }

    @Override
    public void d(String tag, String msg) {
        if (output != null) {
            output.d(getTag(tag), msg);
        }
    }

    @Override
    public void i(String tag, String msg) {
        if (output != null) {
            output.i(getTag(tag), msg);
        }
    }

    @Override
    public void w(String tag, String msg) {
        if (output != null) {
            output.w(getTag(tag), msg);
        }
    }

    @Override
    public void e(String tag, String msg) {
        if (output != null) {
            output.e(getTag(tag), msg);
        }
    }

    @Override
    public void e(String tag, String msg, Throwable e) {
        if (output != null) {
            output.e(getTag(tag), msg, e);
        }
    }
}
