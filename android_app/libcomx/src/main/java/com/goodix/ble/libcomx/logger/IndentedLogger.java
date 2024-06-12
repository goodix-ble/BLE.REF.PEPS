package com.goodix.ble.libcomx.logger;

import com.goodix.ble.libcomx.ILogger;

/**
 * 层叠日志输出器，用于显示日志的层级输出关系
 */
public class IndentedLogger implements ILogger {
    private ILogger output;
    private String msgIndent = null; // 消息的前置空格
    private String defaultTag = "null";

    public IndentedLogger(ILogger output) {
        this.output = output;
    }

    public IndentedLogger(ILogger output, String indent) {
        this.output = output;
        this.msgIndent = indent;
    }

    public void setDefaultTag(String defaultTag) {
        if (defaultTag == null) {
            return;
        }
        this.defaultTag = defaultTag;
    }

    @Override
    public ILogger subLogger() {
        IndentedLogger logger = new IndentedLogger(output); // 因为已经增加了一级，就直接给最终的输出接口

        String msgIndent = this.msgIndent;
        if (msgIndent != null) {
            logger.msgIndent = msgIndent + msgIndent; // 再增加一级
        } else {
            logger.msgIndent = "  "; // 默认为2个空格
        }

        logger.defaultTag = this.defaultTag;

        return logger;
    }

    @Override
    public void v(String tag, String msg) {
        if (output != null) {
            output.v(tag == null ? defaultTag : tag, msgIndent != null ? msgIndent + msg : msg);
        }
    }

    @Override
    public void d(String tag, String msg) {
        if (output != null) {
            output.d(tag == null ? defaultTag : tag, msgIndent != null ? msgIndent + msg : msg);
        }
    }

    @Override
    public void i(String tag, String msg) {
        if (output != null) {
            output.i(tag == null ? defaultTag : tag, msgIndent != null ? msgIndent + msg : msg);
        }
    }

    @Override
    public void w(String tag, String msg) {
        if (output != null) {
            output.w(tag == null ? defaultTag : tag, msgIndent != null ? msgIndent + msg : msg);
        }
    }

    @Override
    public void e(String tag, String msg) {
        if (output != null) {
            output.e(tag == null ? defaultTag : tag, msgIndent != null ? msgIndent + msg : msg);
        }
    }

    @Override
    public void e(String tag, String msg, Throwable e) {
        if (output != null) {
            output.e(tag == null ? defaultTag : tag, msgIndent != null ? msgIndent + msg : msg, e);
        }
    }
}
