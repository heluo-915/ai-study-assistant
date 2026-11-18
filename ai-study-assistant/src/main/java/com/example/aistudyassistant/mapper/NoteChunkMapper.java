package com.example.aistudyassistant.mapper;

import com.example.aistudyassistant.dto.ChunkPreview;
import com.example.aistudyassistant.entity.NoteChunk;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NoteChunkMapper {

    // 统计当前用户的笔记片段数量。
    @Select("SELECT COUNT(*) FROM note_chunks c JOIN notes n ON n.id=c.note_id WHERE n.user_id=#{userId}")
    int countOwnedChunks(@Param("userId") Long userId);

    // 只要存在未同步的片段或模型不一致，就不能依赖 Qdrant 作为完整索引。
    @Select("""
        SELECT COUNT(*) FROM note_chunks c
        JOIN notes n ON n.id=c.note_id
        LEFT JOIN note_vector_sync_tasks t ON t.note_id=n.id AND t.user_id=n.user_id
        WHERE n.user_id=#{userId}
          AND (t.note_id IS NULL OR t.status <> 'SUCCEEDED'
               OR t.requested_action <> 'UPSERT' OR c.model_name <> #{modelName})
        """)
    int countChunksNotReady(@Param("userId") Long userId,@Param("modelName") String modelName);

    // 从权威数据库重新检查当前用户、片段和正文。
    @Select("""
        SELECT n.id AS noteId,n.title AS noteTitle,c.chunk_index AS chunkIndex,
               c.content,c.model_name AS modelName
        FROM note_chunks c JOIN notes n ON n.id=c.note_id
        WHERE n.user_id=#{userId} AND n.id=#{noteId} AND c.chunk_index=#{chunkIndex}
        """)
    NoteChunk findOwnedChunk(@Param("userId") Long userId,@Param("noteId") Long noteId,
                             @Param("chunkIndex") int chunkIndex);

    // 保存一条笔记片段及其向量。
    @Insert("""
            INSERT INTO note_chunks
                (note_id, chunk_index, content, embedding, model_name)
            VALUES
                (#{noteId}, #{chunkIndex}, #{content}, #{embedding}, #{modelName})
            """)
    int insert(
            @Param("noteId") Long noteId,
            @Param("chunkIndex") int chunkIndex,
            @Param("content") String content,
            @Param("embedding") String embedding,
            @Param("modelName") String modelName
    );

    // 删除指定笔记的所有片段。
    @Delete("DELETE FROM note_chunks WHERE note_id = #{noteId}")
    int deleteByNoteId(@Param("noteId") Long noteId);
    // 查询当前用户笔记的所有片段及其向量。
    @Select("""
        SELECT n.id AS noteId,
               n.title AS noteTitle,
               c.chunk_index AS chunkIndex,
               c.content,
               c.embedding,
               c.model_name AS modelName
        FROM note_chunks c
        JOIN notes n ON c.note_id = n.id
        WHERE n.user_id = #{userId}
        ORDER BY n.id, c.chunk_index
        """)
    List<NoteChunk> findByUserId(@Param("userId") Long userId);
    // 查询当前用户指定笔记的片段正文，并按顺序排列。
    @Select("""
        SELECT c.chunk_index AS chunkIndex, c.content
        FROM note_chunks c
        JOIN notes n ON c.note_id = n.id
        WHERE n.id = #{noteId}
          AND n.user_id = #{userId}
        ORDER BY c.chunk_index
        """)
    List<ChunkPreview> findPreviewsByNoteIdAndUserId(
            @Param("noteId") Long noteId,
            @Param("userId") Long userId
    );
}
