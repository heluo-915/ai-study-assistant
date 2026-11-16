package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.QuizOption;
import com.example.aistudyassistant.dto.WrongQuizResponse;
import com.example.aistudyassistant.mapper.QuizQuestionMapper;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.util.Arrays;
import java.util.List;

@Service
public class WrongQuizService {
    private final QuizQuestionMapper mapper;
    private final JsonMapper jsonMapper = new JsonMapper();

    public WrongQuizService(QuizQuestionMapper mapper) { this.mapper = mapper; }

    // 一道曾答错的题目只返回一次，重练前不显示答案与解析。
    public List<WrongQuizResponse> findWrongQuestions(Long userId) {
        return findWrongQuestions(userId, null);
    }

    // 可选择按最近一次作答状态筛选；null 表示不筛选。
    public List<WrongQuizResponse> findWrongQuestions(Long userId, Boolean latestCorrect) {
        return mapper.findWrongByUserId(userId, latestCorrect).stream().map(quiz -> new WrongQuizResponse(
                quiz.getId(), quiz.getNoteId(), quiz.getQuestion(),
                Arrays.asList(jsonMapper.readValue(quiz.getOptions(), QuizOption[].class)),
                quiz.isLatestCorrect())).toList();
    }
}
