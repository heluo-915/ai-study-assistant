package com.example.aistudyassistant;

import com.example.aistudyassistant.controller.WrongQuizController;
import com.example.aistudyassistant.dto.WrongQuizResponse;
import com.example.aistudyassistant.security.JwtInterceptor;
import com.example.aistudyassistant.service.JwtService;
import com.example.aistudyassistant.service.WrongQuizService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WrongQuizControllerTest {
    // 验证要求令牌并使用已认证用户身份。
    @Test
    void requiresTokenAndUsesAuthenticatedUser() throws Exception {
        WrongQuizService service = mock(WrongQuizService.class);
        JwtService jwt = mock(JwtService.class);
        when(jwt.parseUserId("valid")).thenReturn(5L);
        when(service.findWrongQuestions(5L, null)).thenReturn(List.of());
        var mvc = MockMvcBuilders.standaloneSetup(new WrongQuizController(service))
                .addInterceptors(new JwtInterceptor(jwt)).build();
        mvc.perform(get("/quiz/wrong")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
        mvc.perform(get("/quiz/wrong").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).findWrongQuestions(5L, null);
        when(service.findWrongQuestions(5L, null)).thenReturn(List.of(new WrongQuizResponse(9L, 2L, "题目", List.of(), false)));
        mvc.perform(get("/quiz/wrong").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].quizId").value(9))
                .andExpect(jsonPath("$[0].latestCorrect").value(false))
                .andExpect(jsonPath("$[0].correctAnswer").doesNotExist())
                .andExpect(jsonPath("$[0].explanation").doesNotExist());
        when(service.findWrongQuestions(5L, false)).thenReturn(List.of());
        mvc.perform(get("/quiz/wrong?latestCorrect=false").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk());
        verify(service).findWrongQuestions(5L, false);
        mvc.perform(get("/quiz/wrong?latestCorrect=invalid").header("Authorization", "Bearer valid"))
                .andExpect(status().isBadRequest());
    }
}
