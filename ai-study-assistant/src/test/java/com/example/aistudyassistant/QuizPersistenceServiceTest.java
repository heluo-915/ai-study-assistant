package com.example.aistudyassistant;

import com.example.aistudyassistant.dto.QuizOption;
import com.example.aistudyassistant.dto.QuizQuestion;
import com.example.aistudyassistant.service.QuizPersistenceService;
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
class QuizPersistenceServiceTest {
    @Autowired QuizPersistenceService service;
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
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM quiz_questions WHERE id=? AND note_id=? AND JSON_LENGTH(options)=1", Integer.class, id, note));
        assertTrue(service.save(user + 1000000, note, "original", question).isEmpty());
        assertTrue(service.save(user, note, "changed", question).isEmpty());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM quiz_questions WHERE note_id=?", Integer.class, note));
    }
}
