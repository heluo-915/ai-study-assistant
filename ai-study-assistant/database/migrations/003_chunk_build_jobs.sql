-- 在现有项目数据库中执行一次；先备份数据。
CREATE TABLE chunk_build_jobs (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 note_id BIGINT NOT NULL,
 status VARCHAR(16) NOT NULL,
 attempts INT NOT NULL DEFAULT 0,
 error_message VARCHAR(255),
 worker_token VARCHAR(36),
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 INDEX idx_chunk_jobs_status_id (status,id),
 CONSTRAINT fk_chunk_job_user FOREIGN KEY (user_id) REFERENCES users(id),
 CONSTRAINT fk_chunk_job_note FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
