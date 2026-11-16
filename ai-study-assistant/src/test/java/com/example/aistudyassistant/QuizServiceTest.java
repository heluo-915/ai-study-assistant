package com.example.aistudyassistant;

import com.example.aistudyassistant.exception.ModelServiceException;
import com.example.aistudyassistant.service.ChatService;
import com.example.aistudyassistant.service.QuizService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class QuizServiceTest {
    private static final String VALID = """
        {"question":"接口是什么？","options":[
          {"label":"A","content":"行为约定"},{"label":"B","content":"文件"},
          {"label":"C","content":"数据库"},{"label":"D","content":"网络"}],
         "correctAnswer":"A","explanation":"接口定义行为。"}
        """;

    @Test
    void parsesValidQuestionAndPassesNoteToModel() {
        ChatService chat = mock(ChatService.class);
        when(chat.ask(anyString())).thenReturn(VALID);
        var result = new QuizService(chat).generateQuestion("接口定义行为。").orElseThrow();
        assertEquals("接口是什么？", result.getQuestion());
        assertEquals(4, result.getOptions().size());
        assertEquals("A", result.getCorrectAnswer());
        verify(chat).ask(org.mockito.ArgumentMatchers.contains("接口定义行为。"));
    }

    @Test
    void insufficientInformationReturnsEmpty() {
        ChatService chat = mock(ChatService.class);
        when(chat.ask(anyString())).thenReturn("{\"error\":\"笔记内容不足\"}");
        assertTrue(new QuizService(chat).generateQuestion("短").isEmpty());
    }

    @Test
    void rejectsMalformedAndInvalidQuestions() {
        ChatService chat = mock(ChatService.class);
        QuizService service = new QuizService(chat);
        for (String raw : new String[] {"not json", "[]", "{}", VALID.replace("\"label\":\"D\"", "\"label\":\"C\""),
                VALID.replace("\"correctAnswer\":\"A\"", "\"correctAnswer\":\"E\""),
                VALID.replace("接口是什么？", " "), "{\"error\":\"other\"}"}) {
            when(chat.ask(anyString())).thenReturn(raw);
            assertThrows(ModelServiceException.class, () -> service.generateQuestion("笔记"));
        }
    }
}
