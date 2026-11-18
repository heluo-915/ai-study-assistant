package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.QuizCreatedResponse;
import com.example.aistudyassistant.dto.QuizOption;
import com.example.aistudyassistant.mapper.QuizQuestionMapper;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class NoteQuizListService {
    private final NoteService noteService;
    private final QuizQuestionMapper quizQuestionMapper;
    private final JsonMapper jsonMapper = new JsonMapper();

    // 初始化 NoteQuizListService 实例。
    public NoteQuizListService(NoteService noteService, QuizQuestionMapper quizQuestionMapper) {
        this.noteService = noteService;
        this.quizQuestionMapper = quizQuestionMapper;
    }

    // 检查归属后获取题目；未生成过题目时返回空列表。
    public Optional<List<QuizCreatedResponse>> list(Long userId, Long noteId) {
        return list(userId, noteId, null);
    }

    // beforeId 不为空时只读取比该题更早的记录。
    public Optional<List<QuizCreatedResponse>> list(Long userId, Long noteId, Long beforeId) {
        if (noteService.findByIdAndUserId(userId, noteId) == null) return Optional.empty();
        return Optional.of(quizQuestionMapper.findVisibleByNoteIdAndUserId(noteId, userId, beforeId).stream()
                .map(quiz -> new QuizCreatedResponse(quiz.getId(), quiz.getQuestion(),
                        Arrays.asList(jsonMapper.readValue(quiz.getOptions(), QuizOption[].class))))
                .toList());
    }
}
