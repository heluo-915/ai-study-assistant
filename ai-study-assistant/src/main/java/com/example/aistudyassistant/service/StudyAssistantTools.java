package com.example.aistudyassistant.service;
import com.example.aistudyassistant.dto.ChunkSearchResult;
import com.example.aistudyassistant.dto.QuizStatisticsResponse;
import com.example.aistudyassistant.dto.WrongQuizResponse;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Component
public class StudyAssistantTools {
    private static final Logger log=LoggerFactory.getLogger(StudyAssistantTools.class);
    private final NoteChunkSearchService search;
    private final WrongQuizService wrong;
    private final QuizStatisticsService statistics;
    public StudyAssistantTools(NoteChunkSearchService search, WrongQuizService wrong, QuizStatisticsService statistics) {
        this.search=search; this.wrong=wrong; this.statistics=statistics;
    }
    // 用户身份来自服务器上下文，不由模型指定。
    private Long userId(ToolContext context) {
        Object id = context.getContext().get("userId");
        if (!(id instanceof Long userId) || userId <= 0) throw new IllegalStateException("缺少登录身份");
        return userId;
    }
    @Tool(description="查找当前登录用户与问题相关的笔记片段，结果仅供参考")
    public List<ChunkSearchResult> searchNotes(String question, ToolContext context) {
        Long userId=userId(context);
        if (question == null || question.isBlank() || question.length()>500) throw new IllegalArgumentException("问题长度无效");
        return search.search(userId, question);
    }
    @Tool(description="查询当前登录用户最近仍未答对的错题，不包含正确答案")
    public List<WrongQuizResponse> recentWrongQuestions(ToolContext context) {
        return wrong.findWrongQuestions(userId(context), false);
    }
    @Tool(description="查询当前登录用户的答题次数及正确错误次数")
    public QuizStatisticsResponse quizStatistics(ToolContext context) {
        Long userId=userId(context);
        log.info("Study assistant tool invoked: quizStatistics");
        return statistics.statistics(userId);
    }
}
