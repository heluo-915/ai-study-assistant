package com.example.aistudyassistant;

import com.example.aistudyassistant.service.Utf8LengthCounter;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Utf8LengthCounterTest {
    // 验证分片及拆分代理字符时字节计数与 Java UTF-8 长度一致。
    @Test
    void matchesJavaUtf8LengthAcrossChunksAndSplitSurrogates() {
        String[] chunks = {"abc", "中文", "\uD83D", "", "\uDE00", "é", "\uD800", "x", "\uDC00"};
        Utf8LengthCounter counter = new Utf8LengthCounter();
        StringBuilder combined = new StringBuilder();
        for (String chunk : chunks) {
            combined.append(chunk);
            assertEquals(combined.toString().getBytes(StandardCharsets.UTF_8).length, counter.append(chunk));
        }
    }
}
