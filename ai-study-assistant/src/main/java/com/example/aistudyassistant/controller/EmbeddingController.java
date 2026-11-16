package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.TextSimilarityRequest;
import com.example.aistudyassistant.service.EmbeddingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.aistudyassistant.dto.ChatRequest;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class EmbeddingController {
    private final EmbeddingService embeddingService;

    // 通过构造方法接收并保存所需依赖或初始化字段。
    public EmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping("/embedding/dimensions")
    // 将请求文本向量化并返回向量维度。
    public ResponseEntity<Integer> dimensions(@RequestBody ChatRequest request) {
        String message = request.getMessage();

        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        float[] vector = embeddingService.embed(message);
        return ResponseEntity.ok(vector.length);
    }
    @PostMapping("/embedding/similarity")
    // 校验两段文本并返回它们的余弦相似度。
    public ResponseEntity<Double> similarity(@RequestBody TextSimilarityRequest request) {
        String textA = request.getTextA();
        String textB = request.getTextB();

        // 如果 textA 或 textB 为 null、空字符串或只有空格，返回 400
        if(textA==null||textB==null||textA.isBlank()||textB.isBlank()){
            return ResponseEntity.badRequest().build();
        }
        // 否则调用 embeddingService.textSimilarity，返回结果和 200 状态码
        return ResponseEntity.ok().body(embeddingService.textSimilarity(textA,textB));
    }
}
