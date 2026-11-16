package com.example.aistudyassistant.dto;
import java.time.LocalDate;
public record StudyTask(Long id, Long quizId, String question, LocalDate studyDate, boolean completed) {}
