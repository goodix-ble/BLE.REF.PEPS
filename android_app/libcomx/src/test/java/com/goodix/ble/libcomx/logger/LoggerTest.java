package com.goodix.ble.libcomx.logger;

import com.goodix.ble.libcomx.ILogger;

import org.junit.Test;

public class LoggerTest {

    public static final int COUNT = 100_0000;

    @Test
    public void loggerWithPlus() {
        // 测试性能消耗
        ILogger logger = new FakeLogger();
        for (int i = 0; i < COUNT; i++) {
            Logger.v(logger, "LoggerTest", "loggerWithoutFormatter -> " + i + " - " + i + " - " + i);
        }
    }

    @Test
    public void loggerWithFormatter() {
        // 测试性能消耗
        ILogger logger = new FakeLogger();
        for (int i = 0; i < COUNT; i++) {
            Logger.v(logger, "LoggerTest", "loggerWithFormatter -> %d - %d - %d", i, i, i);
        }
    }

    @Test
    public void loggerWithIf() {
        // 测试性能消耗
        ILogger logger = new FakeLogger();
        for (int i = 0; i < COUNT; i++) {
            if (logger != null) {
                logger.v("LoggerTest", "loggerWithoutFormatter -> " + i + " - " + i + " - " + i);
            }
        }
    }

    @Test
    public void nullLoggerWithPlus() {
        // 测试性能消耗
        for (int i = 0; i < COUNT; i++) {
            Logger.v(null, "LoggerTest", "loggerWithoutFormatter -> " + i + " - " + i + " - " + i);
        }
    }

    @Test
    public void nullLoggerWithFormatter() {
        // 测试性能消耗
        for (int i = 0; i < COUNT; i++) {
            Logger.v(null, "LoggerTest", "loggerWithFormatter -> %d - %d - %d", i, i, i);
        }
    }

    @Test
    public void nullLoggerWithIf() {
        // 测试性能消耗
        ILogger logger = null;
        for (int i = 0; i < COUNT; i++) {
            if (logger != null) {
                logger.v("LoggerTest", "loggerWithoutFormatter -> " + i + " - " + i + " - " + i);
            }
        }
    }

    static class FakeLogger implements ILogger {

        @Override
        public void v(String tag, String msg) {
            System.out.println(tag + ": " + msg);
        }

        @Override
        public void d(String tag, String msg) {
            System.out.println(tag + ": " + msg);
        }

        @Override
        public void i(String tag, String msg) {
            System.out.println(tag + ": " + msg);
        }

        @Override
        public void w(String tag, String msg) {
            System.out.println(tag + ": " + msg);
        }

        @Override
        public void e(String tag, String msg) {
            System.out.println(tag + ": " + msg);
        }

        @Override
        public void e(String tag, String msg, Throwable e) {
            System.out.println(tag + ": " + msg + " " + e);
        }
    }
}