package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.QuizQuestion;
import com.example.aistudyassistant.entity.QuizRecord;
import com.example.aistudyassistant.mapper.QuizQuestionMapper;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.util.Optional;

@Service
public class QuizPersistenceService {
    private final QuizQuestionMapper mapper;
    private final JsonMapper jsonMapper = new JsonMapper();

    // 初始化 QuizPersistenceService 实例。
    public QuizPersistenceService(QuizQuestionMapper mapper) { this.mapper = mapper; }

    // 仅在笔记仍属于用户且正文未变化时保存题目。
    public Optional<Long> save(Long userId, Long noteId, String content, QuizQuestion question) {
        QuizRecord record = new QuizRecord();
        record.setNoteId(noteId);
        record.setQuestion(question.getQuestion());
        record.setOptions(jsonMapper.writeValueAsString(question.getOptions()));
        record.setCorrectAnswer(question.getCorrectAnswer());
        record.setExplanation(question.getExplanation());
        if (mapper.insertForUnchangedNote(record, userId, content) == 0) return Optional.empty();
        if (record.getId() == null) throw new IllegalStateException("题目已保存，但未获取到题目 ID");
        return Optional.of(record.getId());
    }
}
