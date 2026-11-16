package com.example.aistudyassistant;

import com.example.aistudyassistant.controller.QuizStatisticsController;
import com.example.aistudyassistant.dto.QuizStatisticsResponse;
import com.example.aistudyassistant.security.JwtInterceptor;
import com.example.aistudyassistant.service.JwtService;
import com.example.aistudyassistant.service.QuizStatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class QuizStatisticsControllerTest {
    @Test
    void requiresLoginAndUsesAuthenticatedUser() throws Exception {
        QuizStatisticsService service = mock(QuizStatisticsService.class);
        JwtService jwt = mock(JwtService.class);
        when(jwt.parseUserId("valid")).thenReturn(5L);
        when(service.statistics(5L)).thenReturn(new QuizStatisticsResponse(2, 1, 1));
        var mvc = MockMvcBuilders.standaloneSetup(new QuizStatisticsController(service))
                .addInterceptors(new JwtInterceptor(jwt)).build();
        mvc.perform(get("/quiz/statistics")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
        mvc.perform(get("/quiz/statistics").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAttempts").value(2))
                .andExpect(jsonPath("$.correctAttempts").value(1))
                .andExpect(jsonPath("$.incorrectAttempts").value(1));
        verify(service).statistics(5L);
    }
}
