package com.example.aistudyassistant;

import com.example.aistudyassistant.controller.QuizController;
import com.example.aistudyassistant.dto.QuizQuestion;
import com.example.aistudyassistant.entity.Note;
import com.example.aistudyassistant.security.JwtInterceptor;
import com.example.aistudyassistant.service.JwtService;
import com.example.aistudyassistant.service.NoteService;
import com.example.aistudyassistant.service.QuizService;
import com.example.aistudyassistant.service.QuizPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class QuizControllerTest {
    @Test
    void checksAuthenticationOwnershipAndResult() throws Exception {
        NoteService notes = mock(NoteService.class);
        QuizService quiz = mock(QuizService.class);
        QuizPersistenceService persistence = mock(QuizPersistenceService.class);
        JwtService jwt = mock(JwtService.class);
        when(jwt.parseUserId("valid")).thenReturn(5L);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new QuizController(notes, quiz, persistence))
                .addInterceptors(new JwtInterceptor(jwt)).build();
        mvc.perform(post("/notes/9/quiz")).andExpect(status().isUnauthorized());
        verifyNoInteractions(notes, quiz);
        mvc.perform(post("/notes/9/quiz").header("Authorization", "Bearer valid"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(quiz);
        Note note = new Note();
        note.setContent("笔记正文");
        when(notes.findByIdAndUserId(5L, 9L)).thenReturn(note);
        when(quiz.generateQuestion("笔记正文")).thenReturn(Optional.empty());
        mvc.perform(post("/notes/9/quiz").header("Authorization", "Bearer valid"))
                .andExpect(status().isUnprocessableEntity());
        QuizQuestion question = new QuizQuestion();
        question.setQuestion("测试题目");
        question.setCorrectAnswer("A");
        question.setExplanation("解析");
        when(quiz.generateQuestion("笔记正文")).thenReturn(Optional.of(question));
        when(persistence.save(5L, 9L, "笔记正文", question)).thenReturn(Optional.empty());
        mvc.perform(post("/notes/9/quiz").header("Authorization", "Bearer valid"))
                .andExpect(status().isConflict());
        when(persistence.save(5L, 9L, "笔记正文", question)).thenReturn(Optional.of(12L));
        mvc.perform(post("/notes/9/quiz").header("Authorization", "Bearer valid"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.question").value("测试题目"))
                .andExpect(jsonPath("$.quizId").value(12)).andExpect(jsonPath("$.correctAnswer").doesNotExist())
                .andExpect(jsonPath("$.explanation").doesNotExist());
    }
}
