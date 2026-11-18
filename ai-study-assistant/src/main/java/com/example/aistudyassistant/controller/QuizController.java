package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.QuizQuestion;
import com.example.aistudyassistant.dto.QuizCreatedResponse;
import com.example.aistudyassistant.entity.Note;
import com.example.aistudyassistant.service.NoteService;
import com.example.aistudyassistant.service.QuizService;
import com.example.aistudyassistant.service.QuizPersistenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class QuizController {
    private final NoteService noteService;
    private final QuizService quizService;
    private final QuizPersistenceService quizPersistenceService;

    // 初始化 QuizController 实例。
    public QuizController(NoteService noteService, QuizService quizService, QuizPersistenceService quizPersistenceService) {
        this.noteService = noteService;
        this.quizService = quizService;
        this.quizPersistenceService = quizPersistenceService;
    }

    // 根据当前用户自己的笔记生成单选题。
    @PostMapping("/notes/{noteId}/quiz")
    public ResponseEntity<?> generate(@PathVariable("noteId") Long noteId,
                                      @RequestAttribute("userId") Long userId) {
        Note note = noteService.findByIdAndUserId(userId, noteId);
        if (note == null) {
            return ResponseEntity.notFound().build();
        }
        Optional<QuizQuestion> question = quizService.generateQuestion(note.getContent());
        if (question.isEmpty()) {
            return ResponseEntity.unprocessableEntity().body("笔记内容不足，无法生成题目");
        }
        QuizQuestion generated = question.get();
        Optional<Long> id = quizPersistenceService.save(userId, noteId, note.getContent(), generated);
        if (id.isEmpty()) {
            return ResponseEntity.status(409).body("笔记已修改或删除，请重新生成题目");
        }
        return ResponseEntity.status(201).body(new QuizCreatedResponse(id.get(), generated.getQuestion(), generated.getOptions()));
    }
}
