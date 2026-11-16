-- 在现有项目数据库中执行一次；先备份数据。
CREATE TABLE study_tasks (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 quiz_id BIGINT NOT NULL,
 study_date DATE NOT NULL,
 completed BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY uk_study_task (user_id, quiz_id, study_date),
 CONSTRAINT fk_study_task_user FOREIGN KEY (user_id) REFERENCES users(id),
 CONSTRAINT fk_study_task_quiz FOREIGN KEY (quiz_id) REFERENCES quiz_questions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
