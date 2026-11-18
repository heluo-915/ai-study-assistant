package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.WrongQuizResponse;
import com.example.aistudyassistant.service.WrongQuizService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class WrongQuizController {
    private final WrongQuizService service;
    // 初始化 WrongQuizController 实例。
    public WrongQuizController(WrongQuizService service) { this.service = service; }

    // 返回当前用户最近答错过的题目。
    @GetMapping("/quiz/wrong")
    public List<WrongQuizResponse> findWrongQuestions(@RequestAttribute("userId") Long userId,
            @RequestParam(value = "latestCorrect", required = false) Boolean latestCorrect) {
        return service.findWrongQuestions(userId, latestCorrect);
    }
}
