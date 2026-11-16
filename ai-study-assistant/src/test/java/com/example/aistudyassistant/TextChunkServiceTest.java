package com.example.aistudyassistant;

import com.example.aistudyassistant.service.TextChunkService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextChunkServiceTest {

    private final TextChunkService service = new TextChunkService();

    // 验证相邻片段按指定长度重叠。
    @Test
    void shouldSplitWithOverlap() {
        List<String> result = service.split("ABCDEFGHIJ", 4, 1);

        assertEquals(List.of("ABCD", "DEFG", "GHIJ"), result);
    }

    // 验证短文本和空文本。
    @Test
    void shouldHandleShortAndEmptyText() {
        assertEquals(List.of("你好"), service.split("你好", 4, 1));
        assertEquals(List.of(), service.split("", 4, 1));
    }

    // 验证切分时不会拆开表情的 UTF-16 代理对。
    @Test
    void shouldKeepEmojiIntact() {
        assertEquals(List.of("A😀", "B"), service.split("A😀B", 2, 0));
    }

    // 验证无效参数会被拒绝。
    @Test
    void shouldRejectInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> service.split(null, 4, 1));
        assertThrows(IllegalArgumentException.class, () -> service.split("文字", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.split("文字", 4, -1));
        assertThrows(IllegalArgumentException.class, () -> service.split("文字", 4, 4));
    }
}