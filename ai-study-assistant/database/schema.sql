-- AI 学习助手后端第一版：仅用于初始化一个空数据库。
-- 运行前在客户端选择目标数据库；不包含 CREATE DATABASE、USE、DROP 或用户数据。
-- 从实际 MySQL 8 表结构整理，保留字段、索引、外键与级联规则。
SET NAMES utf8mb4;

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notes` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '唯一标识一条笔记',
  `title` varchar(50) NOT NULL COMMENT '笔记标题',
  `content` text NOT NULL COMMENT '笔记内容',
  `user_id` bigint NOT NULL COMMENT '笔记所属用户的 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '笔记创建时间',
  PRIMARY KEY (`id`),
  KEY `fk_notes_user` (`user_id`),
  CONSTRAINT `fk_notes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `note_embeddings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `note_id` bigint NOT NULL,
  `embedding` json NOT NULL,
  `model_name` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `note_id` (`note_id`),
  CONSTRAINT `note_embeddings_ibfk_1` FOREIGN KEY (`note_id`) REFERENCES `notes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `note_chunks` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '片段的唯一编号',
  `note_id` bigint NOT NULL COMMENT '所属笔记的 ID',
  `chunk_index` int NOT NULL COMMENT '片段在笔记中的顺序，从 0 开始',
  `content` text NOT NULL COMMENT '片段正文',
  `embedding` json NOT NULL COMMENT '片段的向量',
  `model_name` varchar(50) NOT NULL COMMENT '生成向量的模型名称',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_chunks_note_index` (`note_id`,`chunk_index`),
  CONSTRAINT `fk_note_chunks_note` FOREIGN KEY (`note_id`) REFERENCES `notes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `chat_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话的唯一编号',
  `user_id` bigint NOT NULL COMMENT '所属用户的 ID',
  `title` varchar(50) NOT NULL COMMENT '会话标题',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `fk_chat_sessions_user` (`user_id`),
  CONSTRAINT `fk_chat_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `chat_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息编号，也用于确定先后顺序',
  `session_id` bigint NOT NULL COMMENT '所属对话',
  `role` varchar(20) NOT NULL COMMENT 'user 或 assistant',
  `content` text NOT NULL COMMENT '消息内容',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `fk_chat_messages_session` (`session_id`),
  CONSTRAINT `fk_chat_messages_session` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `quiz_questions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '题目 ID',
  `note_id` bigint NOT NULL COMMENT '题目来源笔记 ID',
  `question` text NOT NULL COMMENT '题目内容',
  `options` json NOT NULL COMMENT '选项列表',
  `correct_answer` varchar(1) NOT NULL COMMENT '正确选项标记',
  `explanation` text NOT NULL COMMENT '答案解析',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `fk_quiz_question_note` (`note_id`),
  CONSTRAINT `fk_quiz_question_note` FOREIGN KEY (`note_id`) REFERENCES `notes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `quiz_attempts` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '作答记录 ID',
  `quiz_id` bigint NOT NULL COMMENT '作答的题目 ID',
  `user_id` bigint NOT NULL COMMENT '作答用户 ID',
  `selected_answer` varchar(1) NOT NULL COMMENT '用户选择的选项',
  `is_correct` tinyint(1) NOT NULL COMMENT '本次作答是否正确',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作答时间',
  PRIMARY KEY (`id`),
  KEY `fk_quiz_attempt_question` (`quiz_id`),
  KEY `fk_quiz_attempt_user` (`user_id`),
  CONSTRAINT `fk_quiz_attempt_question` FOREIGN KEY (`quiz_id`) REFERENCES `quiz_questions` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_quiz_attempt_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
