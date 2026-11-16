-- 为同步任务增加有限次数的自动重试；只扩展已有任务表。
ALTER TABLE note_vector_sync_tasks
    ADD COLUMN attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN next_retry_at DATETIME NULL;
