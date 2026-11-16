package com.example.aistudyassistant.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.example.aistudyassistant.dto.QuizAttemptResponse;
import com.example.aistudyassistant.dto.QuizStatisticsResponse;
import java.util.List;

@Mapper
public interface QuizAttemptMapper {
    // 插入一次作答记录。
    @Insert("""
        INSERT INTO quiz_attempts (quiz_id, user_id, selected_answer, is_correct)
        VALUES (#{quizId}, #{userId}, #{selectedAnswer}, #{correct})
        """)
    int insert(@Param("quizId") Long quizId, @Param("userId") Long userId,
               @Param("selectedAnswer") String selectedAnswer, @Param("correct") boolean correct);

    // 再次限定用户和题目归属，按记录 ID 从新到旧读取。
    @Select("""
        <script>
        SELECT a.id AS attemptId, a.selected_answer AS selectedAnswer,
               a.is_correct AS correct, a.created_at AS createdAt
        FROM quiz_attempts a
        JOIN quiz_questions q ON a.quiz_id = q.id
        JOIN notes n ON q.note_id = n.id
        WHERE a.quiz_id = #{quizId} AND a.user_id = #{userId} AND n.user_id = #{userId}
        <if test="beforeId != null">
            AND a.id &lt; #{beforeId}
        </if>
        ORDER BY a.id DESC LIMIT 50
        </script>
        """)
    List<QuizAttemptResponse> findByQuizIdAndUserId(@Param("quizId") Long quizId, @Param("userId") Long userId,
                                                    @Param("beforeId") Long beforeId);

    // 同时限定作答记录的用户和笔记归属，无记录时返回三个零。
    @Select("""
        SELECT COUNT(*) AS totalAttempts,
               COALESCE(SUM(CASE WHEN a.is_correct = 1 THEN 1 ELSE 0 END), 0) AS correctAttempts,
               COALESCE(SUM(CASE WHEN a.is_correct = 0 THEN 1 ELSE 0 END), 0) AS incorrectAttempts
        FROM quiz_attempts a
        JOIN quiz_questions q ON a.quiz_id = q.id
        JOIN notes n ON q.note_id = n.id
        WHERE a.user_id = #{userId} AND n.user_id = #{userId}
        """)
    QuizStatisticsResponse statisticsByUserId(@Param("userId") Long userId);
}
