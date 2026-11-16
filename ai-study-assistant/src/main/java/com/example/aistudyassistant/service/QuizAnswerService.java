package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.QuizAnswerResponse;
import com.example.aistudyassistant.entity.QuizRecord;
import com.example.aistudyassistant.mapper.QuizQuestionMapper;
import com.example.aistudyassistant.mapper.QuizAttemptMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class QuizAnswerService {
    private final QuizQuestionMapper mapper;
    private final QuizAttemptMapper attemptMapper;

    public QuizAnswerService(QuizQuestionMapper mapper, QuizAttemptMapper attemptMapper) {
        this.mapper = mapper;
        this.attemptMapper = attemptMapper;
    }

    // 在同一事务中检查归属、比较答案并保存作答记录。
    @Transactional
    public Optional<QuizAnswerResponse> answer(Long userId, Long quizId, String answer) {
        QuizRecord question = mapper.findByIdAndUserId(quizId, userId);
        if (question == null) return Optional.empty();
        boolean correct = answer.equals(question.getCorrectAnswer());
        if (attemptMapper.insert(quizId, userId, answer, correct) != 1) {
            throw new IllegalStateException("作答记录保存失败");
        }
        return Optional.of(new QuizAnswerResponse(correct, question.getCorrectAnswer(), question.getExplanation()));
    }
}
