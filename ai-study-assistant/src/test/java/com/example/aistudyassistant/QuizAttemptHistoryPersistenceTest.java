package com.example.aistudyassistant;

import com.example.aistudyassistant.dto.QuizOption;
import com.example.aistudyassistant.dto.QuizQuestion;
import com.example.aistudyassistant.service.QuizPersistenceService;
import com.example.aistudyassistant.service.QuizAnswerService;
import com.example.aistudyassistant.service.WrongQuizService;
import com.example.aistudyassistant.service.QuizAttemptHistoryService;
import com.example.aistudyassistant.mapper.QuizAttemptMapper;
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
class QuizAttemptHistoryPersistenceTest {
    @Autowired QuizPersistenceService service;
    @Autowired QuizAnswerService answerService;
    @Autowired WrongQuizService wrongQuizService;
    @Autowired QuizAttemptHistoryService historyService;
    @Autowired QuizAttemptMapper attemptMapper;
    @Autowired JdbcTemplate jdbc;

    // 验证保存生成的编号，并拒绝已修改或不属于用户的笔记。
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
        assertTrue(historyService.findHistory(user, id).orElseThrow().isEmpty());
        assertTrue(historyService.findHistory(user + 1000000, id).isEmpty());
        assertTrue(historyService.findHistory(user, Long.MAX_VALUE).isEmpty());
        assertTrue(answerService.answer(user, id, "A").orElseThrow().correct());
        assertFalse(answerService.answer(user, id, "B").orElseThrow().correct());
        var history = historyService.findHistory(user, id).orElseThrow();
        assertEquals(2, history.size());
        assertEquals("B", history.get(0).selectedAnswer());
        assertFalse(history.get(0).correct());
        assertNotNull(history.get(0).createdAt());
        assertEquals("A", history.get(1).selectedAnswer());
        assertTrue(history.get(1).correct());
        for (int i = 0; i < 50; i++) {
            attemptMapper.insert(id, user, "B", false);
        }
        var page1 = historyService.findHistory(user, id).orElseThrow();
        assertEquals(50, page1.size());
        var page2 = historyService.findHistory(user, id, page1.get(49).attemptId()).orElseThrow();
        assertEquals(2, page2.size());
        assertEquals("B", page2.get(0).selectedAnswer());
        assertEquals("A", page2.get(1).selectedAnswer());
        assertTrue(historyService.findHistory(user + 1000000, id, page1.get(49).attemptId()).isEmpty());
        assertTrue(answerService.answer(user + 1000000, id, "A").isEmpty());
        assertTrue(answerService.answer(user, Long.MAX_VALUE, "A").isEmpty());
        assertEquals(52, jdbc.queryForObject("SELECT COUNT(*) FROM quiz_attempts WHERE quiz_id=? AND user_id=?", Integer.class, id, user));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM quiz_attempts WHERE quiz_id=? AND selected_answer='A' AND is_correct=1", Integer.class, id));
        assertEquals(51, jdbc.queryForObject("SELECT COUNT(*) FROM quiz_attempts WHERE quiz_id=? AND selected_answer='B' AND is_correct=0", Integer.class, id));
        assertFalse(answerService.answer(user, id, "B").orElseThrow().correct());
        assertTrue(answerService.answer(user, id, "A").orElseThrow().correct());
        var wrongQuestions = wrongQuizService.findWrongQuestions(user);
        assertEquals(1, wrongQuestions.size());
        assertEquals(id, wrongQuestions.get(0).quizId());
        assertTrue(wrongQuestions.get(0).latestCorrect());
        assertEquals(1, wrongQuizService.findWrongQuestions(user, true).size());
        assertTrue(wrongQuizService.findWrongQuestions(user, false).isEmpty());
        assertEquals("选项", wrongQuestions.get(0).options().get(0).getContent());
        assertTrue(wrongQuizService.findWrongQuestions(user + 1000000).isEmpty());
        assertFalse(answerService.answer(user, id, "B").orElseThrow().correct());
        assertFalse(wrongQuizService.findWrongQuestions(user).get(0).latestCorrect());
        assertEquals(1, wrongQuizService.findWrongQuestions(user, false).size());
        assertTrue(wrongQuizService.findWrongQuestions(user, true).isEmpty());
        assertTrue(wrongQuizService.findWrongQuestions(user + 1000000, false).isEmpty());
        // 50 道后续答对的题排在前面，验证必须先筛选再 LIMIT。
        for (int i = 0; i < 50; i++) {
            Long otherId = service.save(user, note, "original", question).orElseThrow();
            jdbc.update("INSERT INTO quiz_attempts(quiz_id,user_id,selected_answer,is_correct) VALUES (?,?,?,?)", otherId, user, "B", false);
            jdbc.update("INSERT INTO quiz_attempts(quiz_id,user_id,selected_answer,is_correct) VALUES (?,?,?,?)", otherId, user, "A", true);
        }
        assertEquals(id, wrongQuizService.findWrongQuestions(user, false).get(0).quizId());
        assertEquals(50, wrongQuizService.findWrongQuestions(user, true).size());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM quiz_questions WHERE id=? AND note_id=? AND JSON_LENGTH(options)=1", Integer.class, id, note));
        assertTrue(service.save(user + 1000000, note, "original", question).isEmpty());
        assertTrue(service.save(user, note, "changed", question).isEmpty());
        assertEquals(51, jdbc.queryForObject("SELECT COUNT(*) FROM quiz_questions WHERE note_id=?", Integer.class, note));
    }
}
