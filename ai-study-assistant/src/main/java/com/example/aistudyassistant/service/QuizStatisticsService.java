package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.QuizStatisticsResponse;
import com.example.aistudyassistant.mapper.QuizAttemptMapper;
import org.springframework.stereotype.Service;

@Service
public class QuizStatisticsService {
    private final QuizAttemptMapper mapper;
    // 初始化 QuizStatisticsService 实例。
    public QuizStatisticsService(QuizAttemptMapper mapper) { this.mapper = mapper; }

    // 统计属于当前用户的作答次数。
    public QuizStatisticsResponse statistics(Long userId) { return mapper.statisticsByUserId(userId); }
}
