package com.example.aistudyassistant.mapper;

import com.example.aistudyassistant.entity.QuizRecord;
import com.example.aistudyassistant.entity.WrongQuizRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface QuizQuestionMapper {
    // 再次检查归属和正文，避免生成期间笔记被修改或删除后保存旧题。
    @Insert("""
        INSERT INTO quiz_questions (note_id, question, options, correct_answer, explanation)
        SELECT id, #{quiz.question}, #{quiz.options}, #{quiz.correctAnswer}, #{quiz.explanation}
        FROM notes WHERE id = #{quiz.noteId} AND user_id = #{userId} AND BINARY content = BINARY #{originalContent}
        """)
    @Options(useGeneratedKeys = true, keyProperty = "quiz.id")
    int insertForUnchangedNote(@Param("quiz") QuizRecord quiz, @Param("userId") Long userId,
                               @Param("originalContent") String originalContent);

    // 根据题目 ID 和所属笔记的用户 ID 联合查询。
    @Select("""
        SELECT q.id, q.correct_answer AS correctAnswer, q.explanation
        FROM quiz_questions q JOIN notes n ON q.note_id = n.id
        WHERE q.id = #{quizId} AND n.user_id = #{userId}
        FOR UPDATE
        """)
    QuizRecord findByIdAndUserId(@Param("quizId") Long quizId, @Param("userId") Long userId);

    // 题目预览只读取题干与选项，不查询正确答案。
    @Select("""
        SELECT q.id, q.question, q.options
        FROM quiz_questions q JOIN notes n ON q.note_id = n.id
        WHERE q.id = #{quizId} AND n.user_id = #{userId}
        """)
    QuizRecord findVisibleByIdAndUserId(@Param("quizId") Long quizId, @Param("userId") Long userId);

    // 按笔记和用户查询题干与选项，最近创建的题目在前。
    @Select("""
        <script>
        SELECT q.id, q.question, q.options
        FROM quiz_questions q JOIN notes n ON q.note_id = n.id
        WHERE n.id = #{noteId} AND n.user_id = #{userId}
        <if test="beforeId != null">
            AND q.id &lt; #{beforeId}
        </if>
        ORDER BY q.id DESC LIMIT 50
        </script>
        """)
    List<QuizRecord> findVisibleByNoteIdAndUserId(@Param("noteId") Long noteId, @Param("userId") Long userId,
                                                 @Param("beforeId") Long beforeId);

    // 筛选历史错题及最近作答状态，先筛选再限制结果数量。
    @Select("""
        <script>
        SELECT q.id, q.note_id AS noteId, q.question, q.options,
               latest_attempt.is_correct AS latestCorrect
        FROM quiz_questions q
        JOIN notes n ON q.note_id = n.id
        JOIN (
            SELECT quiz_id, MAX(id) AS latest_wrong_id
            FROM quiz_attempts
            WHERE user_id = #{userId} AND is_correct = 0
            GROUP BY quiz_id
        ) wrong ON wrong.quiz_id = q.id
        JOIN (
            SELECT quiz_id, MAX(id) AS latest_id
            FROM quiz_attempts
            WHERE user_id = #{userId}
            GROUP BY quiz_id
        ) latest ON latest.quiz_id = q.id
        JOIN quiz_attempts latest_attempt ON latest_attempt.id = latest.latest_id
        WHERE n.user_id = #{userId}
        <if test="latestCorrect != null">
            AND latest_attempt.is_correct = #{latestCorrect}
        </if>
        ORDER BY wrong.latest_wrong_id DESC
        LIMIT 50
        </script>
        """)
    List<WrongQuizRecord> findWrongByUserId(@Param("userId") Long userId,
                                           @Param("latestCorrect") Boolean latestCorrect);
}
