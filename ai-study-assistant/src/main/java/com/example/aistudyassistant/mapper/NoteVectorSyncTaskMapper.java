package com.example.aistudyassistant.mapper;

import com.example.aistudyassistant.dto.NoteVectorSyncTask;
import com.example.aistudyassistant.dto.NoteVectorSyncTaskKey;
import org.apache.ibatis.annotations.*;

@Mapper
public interface NoteVectorSyncTaskMapper {
    @Select("""
        SELECT user_id AS userId,note_id AS noteId FROM note_vector_sync_tasks
        WHERE status='PENDING' OR (status='FAILED' AND attempts<3 AND next_retry_at<=CURRENT_TIMESTAMP)
        ORDER BY updated_at,note_id LIMIT 1
        """)
    NoteVectorSyncTaskKey nextPending();

    // 仅为当前用户已有片段且尚无任务记录的笔记安排首次回填。
    @Insert("""
        INSERT INTO note_vector_sync_tasks (note_id,user_id,requested_action,status)
        SELECT n.id,n.user_id,'UPSERT','PENDING' FROM notes n
        WHERE n.user_id=#{userId}
          AND EXISTS (SELECT 1 FROM note_chunks c WHERE c.note_id=n.id)
          AND NOT EXISTS (SELECT 1 FROM note_vector_sync_tasks t WHERE t.note_id=n.id)
        ON DUPLICATE KEY UPDATE requested_version=note_vector_sync_tasks.requested_version
        """)
    int scheduleMissing(@Param("userId") Long userId);
    // 只能为当前用户的现有笔记安排任务；同一笔记多次安排合并为一行并递增版本。
    @Insert("""
        INSERT INTO note_vector_sync_tasks (note_id,user_id,requested_action,status)
        SELECT n.id,n.user_id,#{action},'PENDING' FROM notes n
        WHERE n.id=#{noteId} AND n.user_id=#{userId}
        ON DUPLICATE KEY UPDATE
          attempts=CASE WHEN note_vector_sync_tasks.status='RUNNING' THEN note_vector_sync_tasks.attempts ELSE 0 END,
          next_retry_at=CASE WHEN note_vector_sync_tasks.status='RUNNING' THEN note_vector_sync_tasks.next_retry_at ELSE NULL END,
          requested_version=note_vector_sync_tasks.requested_version+1,
          requested_action=VALUES(requested_action),
          status=CASE WHEN note_vector_sync_tasks.status='RUNNING' THEN 'RUNNING' ELSE 'PENDING' END,
          error_message=NULL
        """)
    int schedule(@Param("userId") Long userId, @Param("noteId") Long noteId, @Param("action") String action);

    @Select("""
        SELECT note_id AS noteId,user_id AS userId,requested_version AS requestedVersion,
               requested_action AS requestedAction,claimed_version AS claimedVersion,
               claimed_action AS claimedAction,status,error_message AS errorMessage,attempts,
               next_retry_at AS nextRetryAt,
               created_at AS createdAt,updated_at AS updatedAt
        FROM note_vector_sync_tasks WHERE note_id=#{noteId} AND user_id=#{userId}
        """)
    NoteVectorSyncTask find(@Param("userId") Long userId, @Param("noteId") Long noteId);

    // 通过状态条件原子领取；新修改不会改变正在处理的版本。
    @Update("""
        UPDATE note_vector_sync_tasks
        SET status='RUNNING',worker_token=#{token},claimed_version=requested_version,
            claimed_action=requested_action,error_message=NULL,attempts=attempts+1,next_retry_at=NULL
        WHERE note_id=#{noteId} AND user_id=#{userId}
          AND (status='PENDING' OR (status='FAILED' AND attempts<3 AND next_retry_at<=CURRENT_TIMESTAMP))
        """)
    int claim(@Param("userId") Long userId,@Param("noteId") Long noteId,@Param("token") String token);

    // 若领取期间又有新版本，只完成当前版本并重新排队。
    @Update("""
        UPDATE note_vector_sync_tasks
        SET status=CASE WHEN requested_version>claimed_version THEN 'PENDING'
                        WHEN #{success} THEN 'SUCCEEDED' ELSE 'FAILED' END,
            error_message=CASE WHEN requested_version>claimed_version OR #{success} THEN NULL
                               ELSE '同步失败，请检查服务日志后重试' END,
            next_retry_at=CASE WHEN requested_version>claimed_version OR #{success} OR attempts>=3 THEN NULL
                               ELSE DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 SECOND) END,
            attempts=CASE WHEN requested_version>claimed_version THEN 0 ELSE attempts END,
            worker_token=NULL, claimed_version=NULL, claimed_action=NULL
        WHERE note_id=#{noteId} AND user_id=#{userId} AND status='RUNNING' AND worker_token=#{token}
        """)
    int finish(@Param("userId") Long userId,@Param("noteId") Long noteId,
               @Param("token") String token,@Param("success") boolean success);

    @Update("""
        UPDATE note_vector_sync_tasks SET status='PENDING',error_message=NULL,attempts=0,next_retry_at=NULL
        WHERE note_id=#{noteId} AND user_id=#{userId} AND status='FAILED'
        """)
    int retry(@Param("userId") Long userId,@Param("noteId") Long noteId);

    // 仅在确认旧执行者已停止后手动调用；按原令牌比较并重置任务。
    @Update("""
        UPDATE note_vector_sync_tasks
        SET status='PENDING',worker_token=NULL,claimed_version=NULL,
            claimed_action=NULL,error_message=NULL,attempts=0,next_retry_at=NULL
        WHERE note_id=#{noteId} AND user_id=#{userId}
          AND status='RUNNING' AND worker_token=#{expectedToken}
        """)
    int recoverInterrupted(@Param("userId") Long userId,@Param("noteId") Long noteId,
                           @Param("expectedToken") String expectedToken);
}
