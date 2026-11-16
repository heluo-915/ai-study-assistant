package com.example.aistudyassistant.service;

import com.example.aistudyassistant.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;
import com.example.aistudyassistant.entity.ChatMessage;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
@Service
public class ChatMessageService {
    private final ChatSessionService chatSessionService;
    private final ChatMessageMapper chatMessageMapper;

    // 通过构造方法接收并保存所需依赖或初始化字段。
    public ChatMessageService(ChatSessionService chatSessionService, ChatMessageMapper chatMessageMapper) {
        this.chatSessionService = chatSessionService;
        this.chatMessageMapper = chatMessageMapper;
    }
    // 先检查会话归属，再读取会话消息。
    public Optional<List<ChatMessage>> findMessages(Long sessionId, Long userId) {
        if (chatSessionService.findByIdAndUserId(sessionId, userId) == null) {
            return Optional.empty();
        }
        List<ChatMessage> messages =chatMessageMapper.findChatMessagesBySessionId(sessionId);

        return Optional.of(messages);
    }
    @Transactional
    // 在事务内保存本轮用户消息和助手回答。
    public void saveConversationTurn(Long sessionId, String question, String answer){
        ChatMessage chatMessage1 = new ChatMessage();
        chatMessage1.setSessionId(sessionId);
        chatMessage1.setRole("user");
        chatMessage1.setContent(question);
        chatMessageMapper.insertChatMessage(chatMessage1);

        ChatMessage chatMessage2 = new ChatMessage();
        chatMessage2.setSessionId(sessionId);
        chatMessage2.setRole("assistant");
        chatMessage2.setContent(answer);
        chatMessageMapper.insertChatMessage(chatMessage2);
    }

    // 检查会话归属，读取最近消息并恢复时间顺序。
    public Optional<List<ChatMessage>> findRecentMessages(
            Long sessionId, Long userId, int limit
    ){
        if (chatSessionService.findByIdAndUserId(sessionId, userId) == null) {
            return Optional.empty();
        }
        List<ChatMessage> messages = chatMessageMapper.findRecentMessagesBySessionId(sessionId, limit);
        Collections.reverse(messages);
        return Optional.of(messages);
    }

}
