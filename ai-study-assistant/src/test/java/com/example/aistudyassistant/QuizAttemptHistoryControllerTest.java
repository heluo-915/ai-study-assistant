package com.example.aistudyassistant;

import com.example.aistudyassistant.controller.QuizAttemptHistoryController;
import com.example.aistudyassistant.security.JwtInterceptor;
import com.example.aistudyassistant.service.JwtService;
import com.example.aistudyassistant.service.QuizAttemptHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class QuizAttemptHistoryControllerTest {
    @Test
    void checksLoginOwnershipAndEmptyHistory() throws Exception {
        QuizAttemptHistoryService service = mock(QuizAttemptHistoryService.class);
        JwtService jwt = mock(JwtService.class);
        when(jwt.parseUserId("valid")).thenReturn(5L);
        var mvc = MockMvcBuilders.standaloneSetup(new QuizAttemptHistoryController(service))
                .addInterceptors(new JwtInterceptor(jwt)).build();
        mvc.perform(get("/quiz/9/attempts")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
        when(service.findHistory(5L, 9L, null)).thenReturn(Optional.empty());
        mvc.perform(get("/quiz/9/attempts").header("Authorization", "Bearer valid")).andExpect(status().isNotFound());
        when(service.findHistory(5L, 9L, null)).thenReturn(Optional.of(List.of()));
        mvc.perform(get("/quiz/9/attempts").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        when(service.findHistory(5L, 9L, 3L)).thenReturn(Optional.of(List.of()));
        mvc.perform(get("/quiz/9/attempts?beforeId=3").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk());
        verify(service).findHistory(5L, 9L, 3L);
        mvc.perform(get("/quiz/9/attempts?beforeId=0").header("Authorization", "Bearer valid"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/quiz/9/attempts?beforeId=bad").header("Authorization", "Bearer valid"))
                .andExpect(status().isBadRequest());
    }
}
