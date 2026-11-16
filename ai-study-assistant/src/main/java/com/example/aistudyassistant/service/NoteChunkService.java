package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.ChunkEmbedding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class NoteChunkService {

    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP = 50;

    @Value("${spring.ai.ollama.embedding.model}")
    private String modelName;

    private final TextChunkService textChunkService;
    private final EmbeddingService embeddingService;

    public NoteChunkService(
            TextChunkService textChunkService,
            EmbeddingService embeddingService
    ) {
        this.textChunkService = textChunkService;
        this.embeddingService = embeddingService;
    }

    // 切分文本，并为每个片段生成向量。
    public List<ChunkEmbedding> generateChunks(String text) {
        List<String> chunks = textChunkService.split(text, CHUNK_SIZE, OVERLAP);
        List<ChunkEmbedding> results = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i);
            float[] vector = embeddingService.embed(content);
            String embeddingJson = Arrays.toString(vector);

            results.add(new ChunkEmbedding(i, content, embeddingJson, modelName));
        }

        return results;
    }
}