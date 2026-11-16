package com.example.aistudyassistant.mapper;
import com.example.aistudyassistant.dto.ChunkBuildJob;
import org.apache.ibatis.annotations.*;
import java.util.Map;

@Mapper
public interface ChunkBuildJobMapper {
    @Insert("""
      INSERT INTO chunk_build_jobs (user_id,note_id,status)
      SELECT #{userId},id,'QUEUED' FROM notes WHERE id=#{noteId} AND user_id=#{userId}
      """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(Map<String, Object> parameters);
    @Select("""
      SELECT id,note_id AS noteId,status,attempts,error_message AS errorMessage,created_at AS createdAt,updated_at AS updatedAt
      FROM chunk_build_jobs WHERE id=#{id} AND user_id=#{userId}
      """)
    ChunkBuildJob find(@Param("userId") Long userId,@Param("id") Long id);
    @Select("SELECT id FROM chunk_build_jobs WHERE status='QUEUED' ORDER BY id LIMIT 1")
    Long nextId();
    @Update("UPDATE chunk_build_jobs SET status='RUNNING', worker_token=#{token}, error_message=NULL, attempts=attempts+1 WHERE id=#{id} AND status='QUEUED'")
    int claim(@Param("id") Long id,@Param("token") String token);
    @Select("SELECT user_id FROM chunk_build_jobs WHERE id=#{id} AND status='RUNNING' AND worker_token=#{token}")
    Long owner(@Param("id") Long id,@Param("token") String token);
    @Select("SELECT note_id FROM chunk_build_jobs WHERE id=#{id} AND status='RUNNING' AND worker_token=#{token}")
    Long note(@Param("id") Long id,@Param("token") String token);
    @Update("UPDATE chunk_build_jobs SET status=#{status},worker_token=NULL,error_message=CASE WHEN #{status}='FAILED' THEN '任务处理失败，请检查服务日志后重试' ELSE NULL END WHERE id=#{id} AND status='RUNNING' AND worker_token=#{token}")
    int finish(@Param("id") Long id,@Param("token") String token,@Param("status") String status);
    @Update("UPDATE chunk_build_jobs SET status='QUEUED',error_message=NULL WHERE id=#{id} AND user_id=#{userId} AND status='FAILED'")
    int retry(@Param("userId") Long userId,@Param("id") Long id);
    @Update("UPDATE chunk_build_jobs SET status='QUEUED',worker_token=NULL WHERE status='RUNNING' AND updated_at < CURRENT_TIMESTAMP - INTERVAL 30 MINUTE")
    int recoverExpired();
}
