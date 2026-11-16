package com.example.aistudyassistant.mapper;

import com.example.aistudyassistant.entity.Note;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NoteMapper {
    @Insert("""
        INSERT INTO notes(title, content, user_id)
        VALUES (#{title}, #{content}, #{userId})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    // 插入笔记并将数据库生成的 ID 回填到对象。
    int insert(Note note);
    @Select("""
        SELECT id, title, content,
               user_id AS userId,
               created_at AS createdAt
        FROM notes
        WHERE user_id = #{userId}
        """)
    // 根据用户 ID 查询所属笔记。
    List<Note> findByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT id, title, content,
               user_id AS userId,
               created_at AS createdAt
        FROM notes
        WHERE user_id = #{userId} AND id=#{noteId}
        """)
    // 根据记录 ID 和用户 ID 联合查找，限制数据归属。
    Note findByIdAndUserId(@Param("userId") Long userId,@Param("noteId")Long noteId);
    @Delete("delete from notes WHERE id = #{noteId} AND user_id = #{userId}")
    // 按笔记 ID 和用户 ID 删除笔记。
    int deleteByIdAndUserId(@Param("userId") Long userId,@Param("noteId")Long noteId);
    @Update("UPDATE notes\n" +
            "SET title = #{title},\n" +
            "    content = #{content}\n" +
            "WHERE id = #{noteId}\n" +
            "  AND user_id = #{userId}")
    // 按笔记 ID 和用户 ID 更新标题与正文。
    int updateNoteByIdAndUserId(@Param("title") String title,@Param("content")String content,@Param("noteId")Long noteId,@Param("userId")Long userId);

    @Select("""
        SELECT id, title, content,
               user_id AS userId,
               created_at AS createdAt
        FROM notes
        WHERE user_id = #{userId}
            AND (
                LOCATE(#{keyword}, title) > 0
                OR LOCATE(#{keyword}, content) > 0
            )
        ORDER BY id DESC
        LIMIT 5
        """)
    // 在当前用户笔记中按关键词匹配标题或正文。
    List<Note> searchByKeyword(@Param("userId") Long userId,@Param("keyword") String keyword);

    // 查询当前用户尚未生成向量的笔记。
    @Select("""
        SELECT n.id, n.title, n.content,
               n.user_id AS userId,
               n.created_at AS createdAt
        FROM notes n
        LEFT JOIN note_embeddings e ON n.id = e.note_id
        WHERE n.user_id = #{userId}
          AND e.note_id IS NULL
        """)
    List<Note> findMissingEmbeddingByUserId(@Param("userId") Long userId);
    // 查询并锁定当前用户的笔记，直到事务结束。
    @Select("""
        SELECT id, title, content,
               user_id AS userId,
               created_at AS createdAt
        FROM notes
        WHERE id = #{noteId} AND user_id = #{userId}
        FOR UPDATE
        """)
    Note findByIdAndUserIdForUpdate(
            @Param("userId") Long userId,
            @Param("noteId") Long noteId
    );
    // 查询当前用户还没有任何片段的笔记。
    @Select("""
        SELECT n.id, n.title, n.content,
               n.user_id AS userId,
               n.created_at AS createdAt
        FROM notes n
        WHERE n.user_id = #{userId}
          AND NOT EXISTS (
              SELECT 1
              FROM note_chunks c
              WHERE c.note_id = n.id
          )
        """)
    List<Note> findMissingChunksByUserId(@Param("userId") Long userId);
}
