package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.QuizStatisticsResponse;
import com.example.aistudyassistant.service.QuizStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuizStatisticsController {
    private final QuizStatisticsService service;
    public QuizStatisticsController(QuizStatisticsService service) { this.service = service; }

    // 返回当前用户的练习次数统计。
    @GetMapping("/quiz/statistics")
    public QuizStatisticsResponse statistics(@RequestAttribute("userId") Long userId) {
        return service.statistics(userId);
    }
}
