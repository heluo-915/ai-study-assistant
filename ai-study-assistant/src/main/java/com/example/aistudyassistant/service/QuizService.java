package com.example.aistudyassistant.service;

import org.springframework.stereotype.Service;
import com.example.aistudyassistant.dto.QuizOption;
import com.example.aistudyassistant.dto.QuizQuestion;
import com.example.aistudyassistant.exception.ModelServiceException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class QuizService {
    private final ChatService chatService;
    private final JsonMapper jsonMapper = new JsonMapper();

    // 初始化 QuizService 实例。
    public QuizService(ChatService chatService) {
        this.chatService = chatService;
    }

    // 根据笔记向模型请求一道单选题，返回尚未解析的 JSON 文本。
    public String generateRawQuestion(String noteContent) {
        String prompt = """
                请根据下面的学习笔记生成一道单选题。

                要求：
                1. 只能根据笔记中的信息出题，不要编造。
                2. 提供 A、B、C、D 四个选项，只有一个正确答案。
                3. 返回 JSON 对象，不要使用 Markdown 代码块。
                4. 如果笔记不足以出题，返回 {"error":"笔记内容不足"}。

                JSON 格式：
                {
                  "question": "题目内容",
                  "options": [
                    {"label": "A", "content": "选项内容"},
                    {"label": "B", "content": "选项内容"},
                    {"label": "C", "content": "选项内容"},
                    {"label": "D", "content": "选项内容"}
                  ],
                  "correctAnswer": "A",
                  "explanation": "答案解析"
                }

                学习笔记：
                %s
                """.formatted(noteContent);
        return chatService.ask(prompt);
    }

    // 内容不足时返回空；模型返回的题目结构不符合要求时抛出模型服务异常。
    public Optional<QuizQuestion> generateQuestion(String noteContent) {
        String raw = generateRawQuestion(noteContent);
        try {
            JsonNode root = jsonMapper.readTree(raw);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("模型返回的内容不是 JSON 对象");
            }
            if (root.has("error")) {
                if (root.size() == 1 && root.get("error").isString()
                        && "笔记内容不足".equals(root.get("error").asString())) {
                    return Optional.empty();
                }
                throw new IllegalArgumentException("模型返回了无法识别的错误");
            }
            if (!isNonBlankText(root.get("question")) || !isNonBlankText(root.get("correctAnswer"))
                    || !isNonBlankText(root.get("explanation")) || !root.has("options")
                    || !root.get("options").isArray() || root.get("options").size() != 4) {
                throw new IllegalArgumentException("题目字段不完整");
            }
            Set<String> labels = new HashSet<>();
            for (JsonNode option : root.get("options")) {
                if (!option.isObject() || !isNonBlankText(option.get("label"))
                        || !isNonBlankText(option.get("content"))) {
                    throw new IllegalArgumentException("选项字段不完整");
                }
                labels.add(option.get("label").asString());
            }
            if (!labels.equals(Set.of("A", "B", "C", "D"))
                    || !labels.contains(root.get("correctAnswer").asString())) {
                throw new IllegalArgumentException("选项标记或正确答案不合法");
            }
            return Optional.of(jsonMapper.readValue(raw, QuizQuestion.class));
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new ModelServiceException("模型返回的题目格式不正确，请重试", exception);
        }
    }

    // 判断 JSON 节点是否为非空文本。
    private boolean isNonBlankText(JsonNode node) {
        return node != null && node.isString() && !node.asString().isBlank();
    }
}
