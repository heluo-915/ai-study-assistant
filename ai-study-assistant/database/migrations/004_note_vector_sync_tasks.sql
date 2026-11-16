-- 每篇笔记一条同步记录；保留删除任务，故不对 notes 建级联外键。
-- 在正确的数据库中执行一次；不修改已有笔记和向量。
CREATE TABLE note_vector_sync_tasks (
    note_id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    requested_version BIGINT NOT NULL DEFAULT 1,
    requested_action VARCHAR(10) NOT NULL,
    claimed_version BIGINT,
    claimed_action VARCHAR(10),
    status VARCHAR(16) NOT NULL,
    worker_token VARCHAR(36),
    error_message VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_vector_sync_status (status, updated_at),
    KEY idx_vector_sync_user (user_id, note_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
