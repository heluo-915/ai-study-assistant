package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.NoteSearchResult;
import com.example.aistudyassistant.entity.NoteEmbedding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class NoteSemanticSearchService {
    @Value("${spring.ai.ollama.embedding.model}")
    private String modelName;
    private static final double MIN_SCORE = 0.5;
    private final NoteEmbeddingService noteEmbeddingService;
    private final EmbeddingService embeddingService;

    public NoteSemanticSearchService(NoteEmbeddingService noteEmbeddingService, EmbeddingService embeddingService) {
        this.noteEmbeddingService = noteEmbeddingService;
        this.embeddingService = embeddingService;
    }

    public List<NoteSearchResult> search(Long userId, String question) {
        // 1. 查询当前用户已有向量的笔记
        List<NoteEmbedding> notes = noteEmbeddingService.findByUserId(userId);

        // 2. 如果没有笔记，就返回空列表
        if (notes.isEmpty()) {
            return List.of();
        }
        // 3. 把问题转换成向量
        float[] questionVector = embeddingService.embed(question);

        // 后面继续计算每篇笔记的相似度
        List<NoteSearchResult> results = new ArrayList<>();

        for (NoteEmbedding note : notes) {
            if (!modelName.equals(note.getModelName())) {
                throw new IllegalStateException(
                        "笔记向量使用的模型与当前模型不一致，笔记 ID：" + note.getNoteId()
                );
            }
            // 把数据库里的 JSON 字符串转换回向量
            float[] noteVector = noteEmbeddingService.parseEmbedding(note.getEmbedding());

            // 计算问题与这篇笔记的相似度
            double score = embeddingService.cosineSimilarity(questionVector, noteVector);

            if (score >= MIN_SCORE) {
                results.add(new NoteSearchResult(
                        note.getNoteId(),
                        note.getTitle(),
                        note.getContent(),
                        score
                ));
            }

        }
        results.sort(
                Comparator.comparingDouble(NoteSearchResult::getScore).reversed()
        );
        return results.subList(0, Math.min(5, results.size()));
    }
}