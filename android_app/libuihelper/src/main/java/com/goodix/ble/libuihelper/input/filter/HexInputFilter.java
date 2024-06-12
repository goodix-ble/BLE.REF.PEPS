package com.goodix.ble.libuihelper.input.filter;

import android.text.InputFilter;
import android.text.Spanned;

public class HexInputFilter implements InputFilter {
    private StringBuilder buffer = new StringBuilder(32);
    private char[] specialChars = null;

    // 增加对特殊字符的支持
    public HexInputFilter setSpecialChars(char... specialChars) {
        this.specialChars = specialChars;
        return this;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        buffer.delete(0, buffer.length());

        int len = source.length();
        for (int i = 0; i < len; i++) {
            char ch = source.charAt(i);
            boolean appendable = false;
            if (ch >= '0' && ch <= '9') {
                appendable = true;
            } else if (ch >= 'A' && ch <= 'F') {
                appendable = true;
            } else if (ch >= 'a' && ch <= 'f') {
                ch = (char) (ch - 'a' + 'A'); // 转换为大写
                appendable = true;
            } else if (specialChars != null) {
                for (char specialChar : specialChars) {
                    if (specialChar == ch) {
                        appendable = true;
                    }
                }
            }

            if (appendable) {
                buffer.append(ch);
            }
        }

        return buffer;
    }
}
