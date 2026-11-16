package com.example.aistudyassistant.service;

import com.example.aistudyassistant.entity.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChatConversationService {
    private final ChatMessageService chatMessageService;
    private final ChatService chatService;

    // 通过构造方法接收并保存所需依赖或初始化字段。
    public ChatConversationService(ChatMessageService chatMessageService, ChatService chatService) {
        this.chatMessageService = chatMessageService;
        this.chatService = chatService;
    }

    // 验证归属并加载近期历史，调用模型后保存本轮对话。
    public Optional<String> ask(Long sessionId, Long userId, String question){
        Optional<List<ChatMessage>> message=chatMessageService.findRecentMessages(sessionId, userId, 20);
        if (message.isEmpty()) {
            return Optional.empty();
        }
        String answer= chatService.askWithHistory(message.get(), question);
        chatMessageService.saveConversationTurn(sessionId, question, answer);
        return Optional.of(answer);
    }
}
