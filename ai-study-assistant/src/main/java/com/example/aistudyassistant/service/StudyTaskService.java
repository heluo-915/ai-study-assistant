package com.example.aistudyassistant.service;
import com.example.aistudyassistant.dto.StudyTask;
import com.example.aistudyassistant.mapper.StudyTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class StudyTaskService {
    private final StudyTaskMapper mapper;
    private final WrongQuizService wrongQuizService;
    // 初始化 StudyTaskService 实例。
    public StudyTaskService(StudyTaskMapper mapper, WrongQuizService wrongQuizService) {
        this.mapper=mapper; this.wrongQuizService=wrongQuizService;
    }
    // 从最近一次仍答错的题目生成当天计划，可重复调用而不重复创建。
    @Transactional
    public List<StudyTask> generate(Long userId, LocalDate date) {
        wrongQuizService.findWrongQuestions(userId, false)
                .forEach(quiz -> mapper.add(userId, quiz.quizId(), date));
        return mapper.findByDate(userId, date);
    }
    // 查询当前用户指定日期的学习任务。
    public List<StudyTask> find(Long userId, LocalDate date) { return mapper.findByDate(userId, date); }
    // 设置Completed 字段。
    public boolean setCompleted(Long userId, Long taskId, boolean completed) {
        return mapper.setCompleted(userId, taskId, completed) > 0;
    }
}
