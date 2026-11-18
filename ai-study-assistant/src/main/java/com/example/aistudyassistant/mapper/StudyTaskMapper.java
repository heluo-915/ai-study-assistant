package com.example.aistudyassistant.mapper;
import com.example.aistudyassistant.dto.StudyTask;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface StudyTaskMapper {
    // 同一天的同一错题只安排一次，并通过笔记所属用户再次检查权限。
    @Insert("""
        INSERT INTO study_tasks (user_id, quiz_id, study_date)
        SELECT #{userId}, q.id, #{studyDate} FROM quiz_questions q
        JOIN notes n ON n.id=q.note_id
        WHERE q.id=#{quizId} AND n.user_id=#{userId}
        ON DUPLICATE KEY UPDATE completed=study_tasks.completed
        """)
    int add(@Param("userId") Long userId, @Param("quizId") Long quizId,
            @Param("studyDate") LocalDate studyDate);

    // 查询当前用户指定日期的学习任务。
    @Select("""
        SELECT t.id, t.quiz_id AS quizId, q.question, t.study_date AS studyDate, t.completed
        FROM study_tasks t JOIN quiz_questions q ON q.id=t.quiz_id
        JOIN notes n ON n.id=q.note_id AND n.user_id=t.user_id
        WHERE t.user_id=#{userId} AND t.study_date=#{studyDate} ORDER BY t.id
        """)
    List<StudyTask> findByDate(@Param("userId") Long userId, @Param("studyDate") LocalDate studyDate);

    // 设置Completed 字段。
    @Update("UPDATE study_tasks SET completed=#{completed} WHERE id=#{taskId} AND user_id=#{userId}")
    int setCompleted(@Param("userId") Long userId, @Param("taskId") Long taskId,
                     @Param("completed") boolean completed);
}
