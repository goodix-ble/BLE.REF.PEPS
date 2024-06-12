package com.goodix.ble.libcomx.util;

import java.util.Formatter;
import java.util.Locale;

/**
 * Provide abilities to convert octet data to string.
 */
@SuppressWarnings({"unused", "WeakerAccess", "UnusedReturnValue"})
public class HexStringBuilder implements CharSequence, Appendable {
    public final static String DEFAULT_SEPARATOR = " ";
    public final static String DEFAULT_PREFIX = "0x";
    public final static String DEFAULT_STRING_FOR_NULL = "null";
    public final static char COMMENT_BEGIN_CHAR = '(';
    public final static char COMMENT_END_CHAR = ')';

    private StringBuilder builder;
    private String octetSeparator;
    private String strForNull = DEFAULT_STRING_FOR_NULL;
    private Formatter formatter;

    public HexStringBuilder() {
        builder = new StringBuilder(32); // 128bit in general
    }

    public HexStringBuilder(int capacity) {
        this.builder = new StringBuilder(capacity);
    }

    public HexStringBuilder(StringBuilder builder) {
        this.builder = builder;
    }

    public StringBuilder getStringBuilder() {
        return builder;
    }

    public HexStringBuilder newLine() {
        builder.append("\n");
        return this;
    }

    public HexStringBuilder useSeparator() {
        return setSeparator(DEFAULT_SEPARATOR);
    }

    public HexStringBuilder setSeparator(String separator) {
        octetSeparator = separator;
        return this;
    }

    public HexStringBuilder putSeparator() {
        if (octetSeparator != null) {
            builder.append(octetSeparator);
        } else {
            builder.append(DEFAULT_SEPARATOR);
        }
        return this;
    }

    public HexStringBuilder setDefaultStringForNull(String strNull) {
        strForNull = strNull;
        return this;
    }

    public HexStringBuilder prefix() {
        builder.append(DEFAULT_PREFIX);
        return this;
    }

    public HexStringBuilder Ox() {
        return prefix();
    }

    public HexStringBuilder comment(String note) {
        builder.append(COMMENT_BEGIN_CHAR).append(note).append(COMMENT_END_CHAR);
        return this;
    }

    public HexStringBuilder comment(String format, Object... args) {
        builder.append(COMMENT_BEGIN_CHAR);
        this.format(format, args);
        builder.append(COMMENT_END_CHAR);
        return this;
    }

    public HexStringBuilder put(byte v) {
        format("%02X", (int) (v & 0xFF));
        return this;
    }

    public HexStringBuilder put(byte[] dat) {
        if (dat == null) {
            return put(strForNull);
        }

        return put(dat, 0, dat.length);
    }

    public HexStringBuilder put(byte[] dat, int pos, int size) {
        if (dat == null) {
            return put(strForNull);
        }

        if (pos < 0) {
            pos = dat.length + pos;
            if (pos < 0) pos = 0;
        }
        if (size < 0) size = 0;

        int end = pos + size;

        if (end > dat.length) end = dat.length;

        for (int i = pos; i < end; ) {
            put(dat[i]);
            i++;
            if (octetSeparator != null && i < end) {
                builder.append(octetSeparator);
            }
        }

        return this;
    }

    public HexStringBuilder dump(byte[] dat, int bytesPerLine) {
        if (dat == null) {
            return put(strForNull);
        }

        return dump(dat, 0, dat.length, bytesPerLine, null, null);
    }

    public HexStringBuilder dump(byte[] dat, int pos, int size
            , int bytesPerLine
            , CharSequence prefixPerLine
            , CharSequence suffixPerLine) {
        if (dat == null) {
            return put(strForNull);
        }

        if (pos < 0) {
            pos = dat.length + pos;
            if (pos < 0) pos = 0;
        }
        if (size < 0) size = 0;

        int end = pos + size;

        if (end > dat.length) end = dat.length;

        size = end - pos;

        if (bytesPerLine < 1) bytesPerLine = 1;

        for (int k = 0; k < size; k += bytesPerLine, pos += bytesPerLine) {
            if (k != 0) {
                newLine();
            }
            // 前缀
            if (prefixPerLine != null) {
                put(prefixPerLine);
            }
            // 输出16进制
            for (int i = 0; i < bytesPerLine; i++) {
                int idx = pos + i;
                if (idx < end) {
                    format("%02X ", 0xFF & dat[idx]);
                } else {
                    put("   ");
                }
            }
            // 输出ASCII码
            for (int i = 0; i < bytesPerLine; i++) {
                int idx = pos + i;
                if (idx < end) {
                    int ch = 0xFF & dat[idx];
                    if (ch < 32) ch = '.';
                    if (ch > 126) ch = '.';
                    builder.append((char) ch);
                } else {
                    put(" ");
                }
            }
            // 后缀
            if (prefixPerLine != null) {
                put(suffixPerLine);
            }
        }

        return this;
    }

    public HexStringBuilder dumpAsAscii(byte[] dat) {
        return dumpAsAscii(dat, 0, -1, '.', '.');
    }

    public HexStringBuilder dumpAsAscii(byte[] dat, int pos, int size, char nonPrintable, char space) {
        if (dat == null) {
            return this;
        }

        if (pos < 0) {
            pos = dat.length + pos;
            if (pos < 0) pos = 0;
        }
        if (size < 0) size = dat.length;

        int end = pos + size;

        if (end > dat.length) end = dat.length;

        size = end - pos;

        for (int i = 0; i < size; i++) {
            int idx = pos + i;
            int ch = 0xFF & dat[idx];
            if (ch < 32) ch = nonPrintable;
            if (ch == 32) ch = space;
            if (ch > 126) ch = nonPrintable;
            builder.append((char) ch);
        }
        return this;
    }

    public HexStringBuilder put(CharSequence v) {
        builder.append(v);
        return this;
    }

    public HexStringBuilder format(String format, Object... args) {
        if (this.formatter == null) {
            this.formatter = new Formatter(builder);
        }
        this.formatter.format(format, args);
        return this;
    }

    public HexStringBuilder put(boolean v) {
        builder.append(v);
        return this;
    }

    public HexStringBuilder put(short v) {
        format("%04X", (int) (v & 0xFFFF));
        return this;
    }

    public HexStringBuilder put(int v) {
        format("%08X", v);
        return this;
    }

    /**
     * 使用大端格式将整数输出为十六进制
     */
    public HexStringBuilder put(int v, int size) {
        if (size < 1) size = 1;
        if (size > 4) size = 4;

        String formatStr = String.format(Locale.US, "%%0%dX", size * 2);
        format(formatStr, v);
        return this;
    }

    @Override
    public int length() {
        return builder.length();
    }

    @Override
    public char charAt(int index) {
        return builder.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return builder.subSequence(start, end);
    }

    @Override
    public String toString() {
        if (builder == null) {
            return super.toString();
        }
        return builder.toString();
    }

    public HexStringBuilder clear() {
        if (builder.length() > 0) {
            builder.delete(0, builder.length());
        }
        return this;
    }

    /**
     * Shortcut for put()
     */
    public HexStringBuilder a(CharSequence s) {
        return put(s);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Delegate methods for StringBuilder
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public HexStringBuilder append(CharSequence s) {
        builder.append(s);
        return this;
    }

    @Override
    public Appendable append(CharSequence s, int start, int end) {
        builder.append(s, start, end);
        return this;
    }

    @Override
    public HexStringBuilder append(char v) {
        builder.append(v);
        return this;
    }

    public HexStringBuilder append(boolean v) {
        builder.append(v);
        return this;
    }

    public HexStringBuilder append(int v) {
        builder.append(v);
        return this;
    }

    public HexStringBuilder append(long v) {
        builder.append(v);
        return this;
    }

    public HexStringBuilder append(float v) {
        builder.append(v);
        return this;
    }

    public HexStringBuilder append(double v) {
        builder.append(v);
        return this;
    }

}
