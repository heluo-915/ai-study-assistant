package com.example.aistudyassistant.mapper;

import com.example.aistudyassistant.entity.NoteEmbedding;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NoteEmbeddingMapper {

    @Insert("""
            INSERT INTO note_embeddings(note_id,embedding,model_name)
            VALUES (#{noteId},#{embedding},#{modelName})
            """)
    // 插入笔记向量 JSON 和生成模型名称。
    int insert(
            // 在这里写三个带 @Param 的参数
            @Param("noteId") Long noteId,
            @Param("embedding") String embedding,
            @Param("modelName") String modelName
    );

    // 笔记没有向量时插入；已有向量时更新。
    @Insert("""
        INSERT INTO note_embeddings (note_id, embedding, model_name)
        VALUES (#{noteId}, #{embedding}, #{modelName})
        ON DUPLICATE KEY UPDATE
            embedding = VALUES(embedding),
            model_name = VALUES(model_name)
        """)
    int upsert(
            @Param("noteId") Long noteId,
            @Param("embedding") String embedding,
            @Param("modelName") String modelName
    );


    // 根据笔记 ID 更新向量和模型名称。
    @Update("""
        UPDATE note_embeddings
        SET embedding = #{embedding},
            model_name = #{modelName}
        WHERE note_id = #{noteId}
        """)
    int updateByNoteId(
            @Param("noteId") Long noteId,
            @Param("embedding") String embedding,
            @Param("modelName") String modelName
    );
    // 查询当前用户笔记及其整篇向量。
    @Select("""
        SELECT n.id AS noteId, n.title, n.content,
               e.embedding, e.model_name AS modelName
        FROM notes n
        JOIN note_embeddings e ON n.id = e.note_id
        WHERE n.user_id = #{userId}
        """)
    List<NoteEmbedding> findByUserId(@Param("userId") Long userId);

}
