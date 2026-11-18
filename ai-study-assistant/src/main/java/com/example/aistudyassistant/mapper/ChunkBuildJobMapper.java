package com.example.aistudyassistant.mapper;
import com.example.aistudyassistant.dto.ChunkBuildJob;
import org.apache.ibatis.annotations.*;
import java.util.Map;

@Mapper
public interface ChunkBuildJobMapper {
    // 插入片段构建任务并回填生成的任务 ID。
    @Insert("""
      INSERT INTO chunk_build_jobs (user_id,note_id,status)
      SELECT #{userId},id,'QUEUED' FROM notes WHERE id=#{noteId} AND user_id=#{userId}
      """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(Map<String, Object> parameters);
    // 按用户和任务 ID 查询构建任务。
    @Select("""
      SELECT id,note_id AS noteId,status,attempts,error_message AS errorMessage,created_at AS createdAt,updated_at AS updatedAt
      FROM chunk_build_jobs WHERE id=#{id} AND user_id=#{userId}
      """)
    ChunkBuildJob find(@Param("userId") Long userId,@Param("id") Long id);
    // 查询下一条等待处理的构建任务 ID。
    @Select("SELECT id FROM chunk_build_jobs WHERE status='QUEUED' ORDER BY id LIMIT 1")
    Long nextId();
    // 通过状态条件和令牌领取构建任务。
    @Update("UPDATE chunk_build_jobs SET status='RUNNING', worker_token=#{token}, error_message=NULL, attempts=attempts+1 WHERE id=#{id} AND status='QUEUED'")
    int claim(@Param("id") Long id,@Param("token") String token);
    // 根据任务 ID 和领取令牌查询所属用户。
    @Select("SELECT user_id FROM chunk_build_jobs WHERE id=#{id} AND status='RUNNING' AND worker_token=#{token}")
    Long owner(@Param("id") Long id,@Param("token") String token);
    // 根据任务 ID 和领取令牌查询关联笔记。
    @Select("SELECT note_id FROM chunk_build_jobs WHERE id=#{id} AND status='RUNNING' AND worker_token=#{token}")
    Long note(@Param("id") Long id,@Param("token") String token);
    // 按领取令牌更新任务结束状态。
    @Update("UPDATE chunk_build_jobs SET status=#{status},worker_token=NULL,error_message=CASE WHEN #{status}='FAILED' THEN '任务处理失败，请检查服务日志后重试' ELSE NULL END WHERE id=#{id} AND status='RUNNING' AND worker_token=#{token}")
    int finish(@Param("id") Long id,@Param("token") String token,@Param("status") String status);
    // 将当前用户失败的构建任务重新排队。
    @Update("UPDATE chunk_build_jobs SET status='QUEUED',error_message=NULL WHERE id=#{id} AND user_id=#{userId} AND status='FAILED'")
    int retry(@Param("userId") Long userId,@Param("id") Long id);
    // 将超时的构建任务重新排队，供后续处理。
    @Update("UPDATE chunk_build_jobs SET status='QUEUED',worker_token=NULL WHERE status='RUNNING' AND updated_at < CURRENT_TIMESTAMP - INTERVAL 30 MINUTE")
    int recoverExpired();
}
