package com.example.aistudyassistant.service;

import com.example.aistudyassistant.entity.ChatMessage;
import com.example.aistudyassistant.exception.ModelServiceException;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.web.client.RestClientException;

@Service
public class ChatService {
    private final ChatModel chatModel;
    // 通过构造方法接收并保存所需依赖或初始化字段。
    public ChatService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    // 向对话模型发送单轮问题。
    public String ask(String message) {
        return callPrompt(new Prompt(message));
    }
    // 将数据库中的用户与助手消息转换为 Spring AI 消息。
    public List<Message> convertHistory(List<ChatMessage> history) {
        List<Message> messages = new ArrayList<>();

        for (ChatMessage chatMessage : history) {
            if ("user".equals(chatMessage.getRole())) {
                messages.add(new UserMessage(chatMessage.getContent()));
            } else if ("assistant".equals(chatMessage.getRole())) {
                messages.add(new AssistantMessage(chatMessage.getContent()));
            } else {
                throw new IllegalArgumentException("不支持的消息角色");
            }
        }

        return messages;
    }

    // 把历史消息和本次问题组合为模型提示词。
    public Prompt buildPrompt(List<ChatMessage> history, String question){
        List<Message> messageList = convertHistory(history);
        messageList.add(new UserMessage(question));
        return new Prompt(messageList);
    }

    // 使用历史消息构造提示词并调用模型。
    public String askWithHistory(List<ChatMessage> history, String question) {
        Prompt prompt = buildPrompt(history, question);
        return callPrompt(prompt);
    }
    // 将参考笔记、用户问题和约束组合为提示词。
    public Prompt buildNotePrompt(String noteContent, String question) {
        SystemMessage systemMessage = new SystemMessage(
                "请根据提供的参考笔记回答用户问题。" +
                        "如果笔记中没有足够的信息，请说明信息不足，不要编造。"
        );

        String userText = """
            参考笔记：%s
            用户问题：%s
            """.formatted(noteContent, question);

        UserMessage userMessage = new UserMessage(userText);

        return new Prompt(List.of(systemMessage, userMessage));
    }
    // 调用模型并检查回答是否存在且非空。
    private String callPrompt(Prompt prompt) {
        ChatResponse response;

        try {
            response = chatModel.call(prompt);
        } catch (RestClientException
                 | NonTransientAiException
                 | TransientAiException exception) {
            throw new ModelServiceException(
                    "对话模型调用失败，请检查服务配置或稍后重试",
                    exception
            );
        }

        if (response.getResult() == null) {
            throw new IllegalStateException("模型没有返回结果");
        }

        String answer = response.getResult().getOutput().getText();

        if (answer == null || answer.isBlank()) {
            throw new IllegalStateException("模型返回的回答为空");
        }

        return answer;
    }
    // 根据笔记和问题构造提示词并取得回答。
    public String askAboutNote(String noteContent, String question){
        Prompt prompt= buildNotePrompt(noteContent, question);
        return callPrompt(prompt);
    }
}
