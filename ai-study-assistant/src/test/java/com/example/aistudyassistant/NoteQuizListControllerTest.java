package com.example.aistudyassistant;

import com.example.aistudyassistant.controller.NoteQuizListController;
import com.example.aistudyassistant.dto.QuizCreatedResponse;
import com.example.aistudyassistant.security.JwtInterceptor;
import com.example.aistudyassistant.service.JwtService;
import com.example.aistudyassistant.service.NoteQuizListService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NoteQuizListControllerTest {
    // 验证登录、归属、空结果以及答案隐藏。
    @Test
    void checksLoginOwnershipEmptyAndHidesAnswers() throws Exception {
        NoteQuizListService service = mock(NoteQuizListService.class);
        JwtService jwt = mock(JwtService.class);
        when(jwt.parseUserId("valid")).thenReturn(5L);
        var mvc = MockMvcBuilders.standaloneSetup(new NoteQuizListController(service))
                .addInterceptors(new JwtInterceptor(jwt)).build();
        mvc.perform(get("/notes/9/quizzes")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
        when(service.list(5L, 9L, null)).thenReturn(Optional.empty());
        mvc.perform(get("/notes/9/quizzes").header("Authorization", "Bearer valid")).andExpect(status().isNotFound());
        when(service.list(5L, 9L, null)).thenReturn(Optional.of(List.of()));
        mvc.perform(get("/notes/9/quizzes").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        when(service.list(5L, 9L, null)).thenReturn(Optional.of(List.of(new QuizCreatedResponse(3L, "题目", List.of()))));
        mvc.perform(get("/notes/9/quizzes").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].quizId").value(3))
                .andExpect(jsonPath("$[0].correctAnswer").doesNotExist())
                .andExpect(jsonPath("$[0].explanation").doesNotExist());
        when(service.list(5L, 9L, 3L)).thenReturn(Optional.of(List.of()));
        mvc.perform(get("/notes/9/quizzes?beforeId=3").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk());
        verify(service).list(5L, 9L, 3L);
        mvc.perform(get("/notes/9/quizzes?beforeId=0").header("Authorization", "Bearer valid"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/notes/9/quizzes?beforeId=invalid").header("Authorization", "Bearer valid"))
                .andExpect(status().isBadRequest());
    }
}
