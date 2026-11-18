package com.example.aistudyassistant.service;

import com.example.aistudyassistant.mapper.ChatSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatStreamPersistenceService {
    private final ChatSessionMapper sessionMapper;
    private final ChatMessageService messageService;

    // 初始化 ChatStreamPersistenceService 实例。
    public ChatStreamPersistenceService(ChatSessionMapper sessionMapper, ChatMessageService messageService) {
        this.sessionMapper = sessionMapper;
        this.messageService = messageService;
    }

    // 短事务内重新确认会话归属，再原子保存本轮问答。
    @Transactional
    public void save(Long sessionId, Long userId, String question, String answer) {
        if (sessionMapper.lockOwnedSessionId(sessionId, userId) == null) {
            throw new IllegalStateException("会话不存在或已失去访问权限");
        }
        messageService.saveConversationTurn(sessionId, question, answer);
    }
}
