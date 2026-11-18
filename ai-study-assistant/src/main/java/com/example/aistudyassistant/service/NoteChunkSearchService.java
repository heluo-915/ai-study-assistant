package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.ChunkSearchResult;
import com.example.aistudyassistant.entity.NoteChunk;
import com.example.aistudyassistant.mapper.NoteChunkMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class NoteChunkSearchService {

    private static final Logger log = LoggerFactory.getLogger(NoteChunkSearchService.class);

    private static final double MIN_SCORE = 0.5;
    // 扩大候选范围，减少相似片段挤掉证据；不做可能丢失冲突事实的去重。
    private static final int MAX_RESULTS = 10;

    @Value("${spring.ai.ollama.embedding.model}")
    private String modelName;
    @Value("${app.qdrant-search.enabled:false}")
    private boolean qdrantEnabled;

    private final NoteChunkMapper noteChunkMapper;
    private final NoteEmbeddingService noteEmbeddingService;
    private final EmbeddingService embeddingService;
    private final NoteChunkVectorService qdrant;

    /* 保存片段查询、向量解析和索引检索所需的依赖。 */ public NoteChunkSearchService(
            NoteChunkMapper noteChunkMapper,
            NoteEmbeddingService noteEmbeddingService,
            EmbeddingService embeddingService,
            NoteChunkVectorService qdrant
    ) {
        this.noteChunkMapper = noteChunkMapper;
        this.noteEmbeddingService = noteEmbeddingService;
        this.embeddingService = embeddingService;
        this.qdrant = qdrant;
    }

    // 按语义相似度搜索当前用户的笔记片段。
    public List<ChunkSearchResult> search(Long userId, String question) {
        if (qdrantEnabled) {
            try {
                List<ChunkSearchResult> indexed = searchVerifiedQdrant(userId, question);
                if (indexed != null) return indexed;
            } catch (RuntimeException failure) {
                log.warn("Qdrant 检索不可用，回退 MySQL；异常类型：{}", failure.getClass().getSimpleName());
            }
        }
        return searchMySql(userId, question);
    }

    // null 表示结果不可证明完整或最新，需要回退；所有文本均以 MySQL 为准。
    private List<ChunkSearchResult> searchVerifiedQdrant(Long userId, String question) {
        if (noteChunkMapper.countChunksNotReady(userId, modelName) != 0) return null;
        int mysqlCount = noteChunkMapper.countOwnedChunks(userId);
        if (qdrant.countByUser(userId) != mysqlCount) return null;
        List<Document> documents = qdrant.searchChunks(userId, question);
        if (documents.isEmpty()) return null;
        List<ChunkSearchResult> results = new ArrayList<>();
        for (Document document : documents) {
            var metadata = document.getMetadata();
            if (!String.valueOf(userId).equals(String.valueOf(metadata.get("userId")))
                    || !modelName.equals(metadata.get("modelName")) || document.getScore() == null) return null;
            long noteId = Long.parseLong(String.valueOf(metadata.get("noteId")));
            int chunkIndex = Integer.parseInt(String.valueOf(metadata.get("chunkIndex")));
            NoteChunk current = noteChunkMapper.findOwnedChunk(userId, noteId, chunkIndex);
            if (current == null || !modelName.equals(current.getModelName())
                    || !java.util.Objects.equals(current.getContent(), document.getText())) return null;
            if (document.getScore() >= MIN_SCORE) {
                results.add(new ChunkSearchResult(current.getNoteId(), current.getNoteTitle(),
                        current.getChunkIndex(), current.getContent(), document.getScore()));
            }
        }
        // 检索期间有新写入时，退回 MySQL 重新检索。
        if (noteChunkMapper.countChunksNotReady(userId, modelName) != 0) return null;
        if (noteChunkMapper.countOwnedChunks(userId) != mysqlCount || qdrant.countByUser(userId) != mysqlCount) return null;
        return results;
    }

    /* 读取用户片段并计算问题向量与片段向量的余弦相似度。 */ private List<ChunkSearchResult> searchMySql(Long userId, String question) {
        List<NoteChunk> chunks = noteChunkMapper.findByUserId(userId);

        if (chunks.isEmpty()) {
            return List.of();
        }

        float[] questionVector = embeddingService.embed(question);
        List<ChunkSearchResult> results = new ArrayList<>();

        for (NoteChunk chunk : chunks) {
            if (!modelName.equals(chunk.getModelName())) {
                throw new IllegalStateException(
                        "笔记片段使用的模型与当前模型不一致，笔记 ID："
                                + chunk.getNoteId()
                );
            }

            float[] chunkVector =
                    noteEmbeddingService.parseEmbedding(chunk.getEmbedding());

            double score =
                    embeddingService.cosineSimilarity(questionVector, chunkVector);

            if (score >= MIN_SCORE) {
                results.add(new ChunkSearchResult(
                        chunk.getNoteId(),
                        chunk.getNoteTitle(),
                        chunk.getChunkIndex(),
                        chunk.getContent(),
                        score
                ));
            }
        }

        results.sort(
                Comparator.comparingDouble(ChunkSearchResult::score).reversed()
        );

        return results.subList(0, Math.min(MAX_RESULTS, results.size()));
    }
}
