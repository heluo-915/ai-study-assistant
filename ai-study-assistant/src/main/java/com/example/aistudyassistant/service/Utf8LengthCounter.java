package com.example.aistudyassistant.service;

// 增量计算拼接后字符串的 UTF-8 字节数，支持代理对跨分片。
public class Utf8LengthCounter {
    private long bytes;
    private boolean previousHighSurrogate;

    // 累加文本的 UTF-8 字节长度，并处理跨片段的代理字符。
    public long append(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLowSurrogate(c) && previousHighSurrogate) {
                // 前一个高代理已按未配对字符计为 1 字节；配对后共 4 字节。
                bytes += 3;
                previousHighSurrogate = false;
            } else {
                previousHighSurrogate = Character.isHighSurrogate(c);
                if (Character.isSurrogate(c)) bytes += 1; // Java UTF-8 替换字节为 '?'
                else if (c <= 0x7f) bytes += 1;
                else if (c <= 0x7ff) bytes += 2;
                else bytes += 3;
            }
        }
        return bytes;
    }
}
