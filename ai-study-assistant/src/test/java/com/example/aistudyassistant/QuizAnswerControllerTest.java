package com.example.aistudyassistant;

import com.example.aistudyassistant.controller.QuizAnswerController;
import com.example.aistudyassistant.dto.QuizAnswerResponse;
import com.example.aistudyassistant.security.JwtInterceptor;
import com.example.aistudyassistant.service.JwtService;
import com.example.aistudyassistant.service.QuizAnswerService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class QuizAnswerControllerTest {
    @Test
    void checksAuthInputOwnershipAndAnswers() throws Exception {
        QuizAnswerService service = mock(QuizAnswerService.class);
        JwtService jwt = mock(JwtService.class);
        when(jwt.parseUserId("valid")).thenReturn(5L);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new QuizAnswerController(service))
                .addInterceptors(new JwtInterceptor(jwt)).build();
        String url = "/quiz/9/answer";
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"A\"}"))
                .andExpect(status().isUnauthorized());
        for (String json : new String[]{"{}", "{\"answer\":\"E\"}", "{\"answer\":\"a\"}", "{\"answer\":\"\"}"}) {
            mvc.perform(post(url).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
        when(service.answer(5L, 9L, "A")).thenReturn(Optional.empty());
        mvc.perform(post(url).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"A\"}"))
                .andExpect(status().isNotFound());
        when(service.answer(5L, 9L, "A")).thenReturn(Optional.of(new QuizAnswerResponse(true, "A", "解析")));
        when(service.answer(5L, 9L, "B")).thenReturn(Optional.of(new QuizAnswerResponse(false, "A", "解析")));
        mvc.perform(post(url).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"A\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.correct").value(true)).andExpect(jsonPath("$.correctAnswer").value("A"));
        mvc.perform(post(url).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"B\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.correct").value(false)).andExpect(jsonPath("$.explanation").value("解析"));
    }
}
