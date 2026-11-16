package com.example.aistudyassistant.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkService {

    // 按 Unicode 码点切分文本，相邻片段保留指定数量的重叠。
    public List<String> split(String text, int chunkSize, int overlap) {
        if (text == null) {
            throw new IllegalArgumentException("文本不能为 null");
        }
        if (chunkSize <= 0 || overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("切分长度和重叠长度不合法");
        }
        if (text.isEmpty()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int total = text.codePointCount(0, text.length());
        int start = 0;

        while (start < total) {
            int end = Math.min(start + chunkSize, total);

            int startIndex = text.offsetByCodePoints(0, start);
            int endIndex = text.offsetByCodePoints(0, end);
            chunks.add(text.substring(startIndex, endIndex));

            if (end == total) {
                break;
            }

            start = end - overlap;
        }

        return chunks;
    }
}