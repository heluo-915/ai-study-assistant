package com.example.aistudyassistant;

import com.example.aistudyassistant.dto.QuizOption;
import com.example.aistudyassistant.dto.QuizQuestion;
import com.example.aistudyassistant.service.QuizPersistenceService;
import com.example.aistudyassistant.service.QuizAnswerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class QuizAnswerPersistenceTest {
    @Autowired QuizPersistenceService service;
    @Autowired QuizAnswerService answerService;
    @Autowired JdbcTemplate jdbc;

    @Test
    void savesGeneratedIdAndRejectsChangedOrForeignNote() {
        String username = "quiz_test_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO users(username,password_hash) VALUES (?,?)", username, "test-only");
        Long user = jdbc.queryForObject("SELECT id FROM users WHERE username=?", Long.class, username);
        jdbc.update("INSERT INTO notes(title,content,user_id) VALUES (?,?,?)", "quiz", "original", user);
        Long note = jdbc.queryForObject("SELECT id FROM notes WHERE user_id=?", Long.class, user);
        QuizOption option = new QuizOption();
        option.setLabel("A"); option.setContent("选项");
        QuizQuestion question = new QuizQuestion();
        question.setQuestion("题目"); question.setOptions(List.of(option));
        question.setCorrectAnswer("A"); question.setExplanation("解析");
        Long id = service.save(user, note, "original", question).orElseThrow();
        assertTrue(answerService.answer(user, id, "A").orElseThrow().correct());
        assertFalse(answerService.answer(user, id, "B").orElseThrow().correct());
        assertTrue(answerService.answer(user + 1000000, id, "A").isEmpty());
        assertTrue(answerService.answer(user, Long.MAX_VALUE, "A").isEmpty());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM quiz_attempts WHERE quiz_id=? AND user_id=?", Integer.class, id, user));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM quiz_attempts WHERE quiz_id=? AND selected_answer='A' AND is_correct=1", Integer.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM quiz_attempts WHERE quiz_id=? AND selected_answer='B' AND is_correct=0", Integer.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM quiz_questions WHERE id=? AND note_id=? AND JSON_LENGTH(options)=1", Integer.class, id, note));
        assertTrue(service.save(user + 1000000, note, "original", question).isEmpty());
        assertTrue(service.save(user, note, "changed", question).isEmpty());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM quiz_questions WHERE note_id=?", Integer.class, note));
    }
}
