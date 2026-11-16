package com.example.aistudyassistant.service;

import com.example.aistudyassistant.exception.ModelServiceException;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class EmbeddingService {
    private final EmbeddingModel embeddingModel;

    // 通过构造方法接收并保存所需依赖或初始化字段。
    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
    // 调用本地模型将文本转换为向量。
    public float[] embed(String text) {
        try {
            return embeddingModel.embed(text);
        } catch (RestClientException
                 | NonTransientAiException
                 | TransientAiException exception) {
            throw new ModelServiceException(
                    "本地向量模型调用失败，请检查 Ollama 后重试",
                    exception
            );
        }
    }
    // 检查向量并根据点积和长度计算余弦相似度。
    public double cosineSimilarity(float[] a, float[] b){
        if(a==null||b==null||a.length!=b.length||a.length==0||b.length==0){
            throw new IllegalArgumentException("向量不能为空且维度必须一致");
        }
        double dot = 0.0;      // 点积
        double normA = 0.0;    // A 各元素平方之和
        double normB = 0.0;    // B 各元素平方之和
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }

        double sNorma= Math.sqrt(normA);
        double sNormb=Math.sqrt(normB);

        if(sNorma==0||sNormb==0){
            throw new IllegalArgumentException("不能计算零向量的余弦相似度");
        }
        double cosine=dot/(sNorma*sNormb);

        return cosine;
    }
    // 将两段文本分别向量化后计算余弦相似度。
    public double textSimilarity(String textA, String textB) {
        // 1. 把 textA 转换为向量 a
        float[] a= embed(textA);
        // 2. 把 textB 转换为向量 b
        float[] b= embed(textB);
        // 3. 计算并返回两个向量的余弦相似度
        return cosineSimilarity(a,b);

    }
}
