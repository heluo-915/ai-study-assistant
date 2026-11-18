package com.example.aistudyassistant;

import com.example.aistudyassistant.controller.QuizReviewController;
import com.example.aistudyassistant.dto.QuizCreatedResponse;
import com.example.aistudyassistant.security.JwtInterceptor;
import com.example.aistudyassistant.service.JwtService;
import com.example.aistudyassistant.service.QuizReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class QuizReviewControllerTest {
    // 验证身份和归属，并确保查看题目时隐藏答案。
    @Test
    void checksAuthenticationOwnershipAndHidesAnswers() throws Exception {
        QuizReviewService service = mock(QuizReviewService.class);
        JwtService jwt = mock(JwtService.class);
        when(jwt.parseUserId("valid")).thenReturn(5L);
        var mvc = MockMvcBuilders.standaloneSetup(new QuizReviewController(service))
                .addInterceptors(new JwtInterceptor(jwt)).build();
        mvc.perform(get("/quiz/9")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
        when(service.findQuestion(5L, 9L)).thenReturn(Optional.empty());
        mvc.perform(get("/quiz/9").header("Authorization", "Bearer valid")).andExpect(status().isNotFound());
        when(service.findQuestion(5L, 9L)).thenReturn(Optional.of(new QuizCreatedResponse(9L, "题目", List.of())));
        mvc.perform(get("/quiz/9").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quizId").value(9))
                .andExpect(jsonPath("$.question").value("题目"))
                .andExpect(jsonPath("$.correctAnswer").doesNotExist())
                .andExpect(jsonPath("$.explanation").doesNotExist());
    }
}
