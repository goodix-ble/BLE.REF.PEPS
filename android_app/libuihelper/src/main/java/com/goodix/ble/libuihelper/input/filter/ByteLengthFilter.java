package com.goodix.ble.libuihelper.input.filter;

import android.text.InputFilter;
import android.text.Spanned;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

@SuppressWarnings("unused")
public class ByteLengthFilter implements InputFilter {
    private final int mMax;
    private final Charset charset;
    private CharBuffer charBuffer;
    private CharsetEncoder gb2312Encoder;
    private final ByteBuffer byteBuffer;

    public ByteLengthFilter(Charset charset, int max) {
        mMax = max;
        this.charset = charset;
        gb2312Encoder = charset.newEncoder();
        charBuffer = CharBuffer.allocate(mMax * 2);
        byteBuffer = ByteBuffer.allocate(max * 3);
    }

    public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                               int dstart, int dend) {
        // 先预判是否会超过
        //int len = dest.length() - (dend - dstart) + (end - start);
        //if (len > mMax) return "";

        // 判断已有的字节数
        int lenAlreadyHave = getCharByteSize(dest, 0, dstart);

        int lenToDel = getCharByteSize(dest, dstart, dend);

        int lenToAdd = getCharByteSize(source, start, end);

        // keep 为剩余可用空间
        int keep = mMax - (lenAlreadyHave - lenToDel);
        if (keep <= 0) {
            return "";
        } else if (keep >= lenToAdd) {
            return null; // 空间足够 keep original
        } else {
            int actualKeep = 0;
            int actualEnd = 0;

            for (int i = start; i < end; i++) {
                int test = getCharByteSize(source, i, i + 1);
                if (test + actualKeep > keep) {
                    break;
                }
                actualEnd = i + 1;
                actualKeep += test;
            }
            charBuffer.clear();
            charBuffer.append(source, start, actualEnd);
            charBuffer.limit(actualEnd - start);
            charBuffer.position(0);
            return charBuffer.toString();
        }
    }

    private int getCharByteSize(CharSequence str, int start, int end) {
        if (start == end) return 0;

        // 防止初始化的时候就被给了一个超长的字符串
        if (end - start > charBuffer.capacity()) {
            end = start + charBuffer.capacity();
        }

        charBuffer.clear();
        charBuffer.append(str, start, end);
        byteBuffer.clear();
        gb2312Encoder.reset();
        charBuffer.position(0); // 归零，准备读取
        charBuffer.limit(end - start);
        CoderResult encode = gb2312Encoder.encode(charBuffer, byteBuffer, true);
        return byteBuffer.position();
    }

    /**
     * @return the maximum length enforced by this input filter
     */
    public int getMax() {
        return mMax;
    }
}
