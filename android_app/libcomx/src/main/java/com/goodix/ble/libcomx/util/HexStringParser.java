package com.goodix.ble.libcomx.util;

import static com.goodix.ble.libcomx.util.HexStringBuilder.COMMENT_BEGIN_CHAR;
import static com.goodix.ble.libcomx.util.HexStringBuilder.COMMENT_END_CHAR;

/**
 * Get byte data from hex format string.
 * Support comment, using brackets by default.
 * e.g. "AB(type) CD(value)23" --> {0xAB, 0xCD, 0x23}
 * e.g. "(offset)0x123ABC" --> {0x12, 0x3A, 0xBC}
 * e.g. "(pos)0x12  (size)0x56" --> {0x12, 0x56}  automatically remove "0x" prefix.
 * e.g. "123" --> {0x12, 0x03}    odd number of character will cause error.
 * e.g. "0x123" --> {0x12, 0x03}
 */
public class HexStringParser {

    public static int parse(CharSequence in, byte[] out, int pos, int size) {

        if (in == null || out == null || pos < 0 || size < 1) {
            return 0;
        }

        int len = in.length();
        int end = pos + size;
        int acc = 0;
        int bitCnt = 0;
        int byteCnt = 0;

        if (end > out.length) {
            end = out.length;
        }

        for (int i = 0; i < len; i++) {
            // 判断是否还有空间存储
            if (pos >= end) {
                break;
            }

            // 取一个字符进行判断
            char ch = in.charAt(i);

            // 去除注释
            if (ch == COMMENT_BEGIN_CHAR && i + 1 < len) {
                int cc = 1; // 支持嵌套注释
                for (i++; i < len; i++) {
                    ch = in.charAt(i);
                    if (ch == COMMENT_BEGIN_CHAR) {
                        cc++;
                        continue;
                    }
                    if (ch == COMMENT_END_CHAR) {
                        cc--;
                        if (cc <= 0) break;
                    }
                }
                continue;
            }

            // 排除 0x 头
            if (ch == '0' && i + 1 < len) {
                char nxtCh = in.charAt(i + 1);
                if (nxtCh == 'x' || nxtCh == 'X') {
                    i++;
                    continue;
                }
            }

            // 转换十六进制值
            if (ch >= '0' && ch <= '9') {
                acc <<= 4;
                bitCnt += 4;
                acc |= (ch - '0');
            } else if (ch >= 'A' && ch <= 'F') {
                acc <<= 4;
                bitCnt += 4;
                acc |= ((ch - 'A') + 0xA);
            } else if (ch >= 'a' && ch <= 'f') {
                acc <<= 4;
                bitCnt += 4;
                acc |= ((ch - 'a') + 0xA);
            }

            if (bitCnt >= 8) {
                bitCnt = 0;

                // 循环的开头已经判断了pos的范围
                out[pos++] = (byte) (acc & 0xff);
                byteCnt++;
                acc = 0;
            }
        }

        // 剩余半个字节
        //noinspection ConstantConditions
        if (bitCnt == 4 && pos < end) {
            out[pos] = (byte) (acc & 0x0f);
            byteCnt++;
        }

        return byteCnt;
    }

    // 将大端格式输入端16进制字符串转换为整数类型数值
    public static int parseInt(CharSequence in, int startPos, int size) {

        if (in == null || startPos < 0 || size < 1) {
            return 0;
        }

        int maxLen = in.length();
        int endPos = startPos + size;
        int acc = 0;

        if (startPos > maxLen) {
            startPos = maxLen;
        }

        if (endPos > maxLen) {
            endPos = maxLen;
        }

        for (int i = startPos; i < endPos; i++) {
            // 取一个字符进行判断
            char ch = in.charAt(i);

            // 去除注释
            if (ch == COMMENT_BEGIN_CHAR && i + 1 < endPos) {
                int cc = 1; // 支持嵌套注释
                for (i++; i < endPos; i++) {
                    ch = in.charAt(i);
                    if (ch == COMMENT_BEGIN_CHAR) {
                        cc++;
                        continue;
                    }
                    if (ch == COMMENT_END_CHAR) {
                        cc--;
                        if (cc <= 0) break;
                    }
                }
                continue;
            }

            // 排除 0x 头
            if (ch == '0' && i + 1 < endPos) {
                char nxtCh = in.charAt(i + 1);
                if (nxtCh == 'x' || nxtCh == 'X') {
                    i++;
                    continue;
                }
            }

            // 转换十六进制值
            if (ch >= '0' && ch <= '9') {
                acc <<= 4;
                acc |= (ch - '0');
            } else if (ch >= 'A' && ch <= 'F') {
                acc <<= 4;
                acc |= ((ch - 'A') + 0xA);
            } else if (ch >= 'a' && ch <= 'f') {
                acc <<= 4;
                acc |= ((ch - 'a') + 0xA);
            }
        }

        return acc;
    }

    public static int parseInt(CharSequence in) {
        if (in == null || in.length() == 0) return 0;
        return parseInt(in, 0, in.length());
    }
}
