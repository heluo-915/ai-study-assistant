package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.QuizCreatedResponse;
import com.example.aistudyassistant.dto.QuizOption;
import com.example.aistudyassistant.entity.QuizRecord;
import com.example.aistudyassistant.mapper.QuizQuestionMapper;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.util.Arrays;
import java.util.Optional;

@Service
public class QuizReviewService {
    private final QuizQuestionMapper mapper;
    private final JsonMapper jsonMapper = new JsonMapper();

    public QuizReviewService(QuizQuestionMapper mapper) { this.mapper = mapper; }

    // 只读取有权查看的题干和选项。
    public Optional<QuizCreatedResponse> findQuestion(Long userId, Long quizId) {
        QuizRecord quiz = mapper.findVisibleByIdAndUserId(quizId, userId);
        if (quiz == null) return Optional.empty();
        return Optional.of(new QuizCreatedResponse(quiz.getId(), quiz.getQuestion(),
                Arrays.asList(jsonMapper.readValue(quiz.getOptions(), QuizOption[].class))));
    }
}
