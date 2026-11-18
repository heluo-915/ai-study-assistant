package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.QuizAttemptResponse;
import com.example.aistudyassistant.mapper.QuizAttemptMapper;
import com.example.aistudyassistant.mapper.QuizQuestionMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class QuizAttemptHistoryService {
    private final QuizQuestionMapper questionMapper;
    private final QuizAttemptMapper attemptMapper;

    // 初始化 QuizAttemptHistoryService 实例。
    public QuizAttemptHistoryService(QuizQuestionMapper questionMapper, QuizAttemptMapper attemptMapper) {
        this.questionMapper = questionMapper;
        this.attemptMapper = attemptMapper;
    }

    // 题目不存在或无权查看时返回空；有权查看但没作答时返回空列表。
    public Optional<List<QuizAttemptResponse>> findHistory(Long userId, Long quizId) {
        return findHistory(userId, quizId, null);
    }

    // 使用作答记录 ID 作为游标读取更早的记录。
    public Optional<List<QuizAttemptResponse>> findHistory(Long userId, Long quizId, Long beforeId) {
        if (questionMapper.findVisibleByIdAndUserId(quizId, userId) == null) return Optional.empty();
        return Optional.of(attemptMapper.findByQuizIdAndUserId(quizId, userId, beforeId));
    }
}
