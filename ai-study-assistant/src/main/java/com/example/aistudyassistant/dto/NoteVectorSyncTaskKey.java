package com.example.aistudyassistant.dto;
// 内部调度所需的任务所属用户及笔记编号。
public record NoteVectorSyncTaskKey(Long userId, Long noteId) {}
