package com.example.aistudyassistant.service;

import com.example.aistudyassistant.entity.NoteEmbedding;
import com.example.aistudyassistant.mapper.NoteEmbeddingMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;

@Service
public class NoteEmbeddingService {
    @Value("${spring.ai.ollama.embedding.model}")
    private String modelName;
    private final EmbeddingService embeddingService;
    private final NoteEmbeddingMapper noteEmbeddingMapper;

    // 通过构造方法接收并保存所需依赖或初始化字段。
    public NoteEmbeddingService(NoteEmbeddingMapper noteEmbeddingMapper, EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
        this.noteEmbeddingMapper = noteEmbeddingMapper;
    }

    // 生成笔记向量，转换为 JSON 后保存及记录模型名称。
    public void saveEmbedding(Long noteId, String text) {
        float[] vector = embeddingService.embed(text);

        // 下一步：把 vector 转成 JSON，再保存
        String embeddingJson = Arrays.toString(vector);
        noteEmbeddingMapper.insert(noteId,embeddingJson,modelName);
    }
    // 重新生成笔记向量，并插入或更新对应记录。
    public int updateEmbedding(Long noteId, String text) {
        return savePreparedEmbedding(noteId, prepareEmbedding(text));
    }
    // 将耗时的模型调用放在数据库事务之前。
    public String prepareEmbedding(String text) {
        return Arrays.toString(embeddingService.embed(text));
    }
    // 保存已经生成的向量；调用者可将其放在短事务中。
    public int savePreparedEmbedding(Long noteId, String embeddingJson) {
        return noteEmbeddingMapper.upsert(noteId, embeddingJson, modelName);
    }
    // 把数据库中的向量 JSON 转换为 float 数组。
    public float[] parseEmbedding(String embeddingJson) {
        JsonMapper jsonMapper = new JsonMapper();
        return jsonMapper.readValue(embeddingJson, float[].class);
    }
    public List<NoteEmbedding> findByUserId(Long userId) {
        // 调用 noteEmbeddingMapper 的同名方法，返回查询结果
        return noteEmbeddingMapper.findByUserId(userId);
    }
}
