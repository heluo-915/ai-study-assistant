package com.example.aistudyassistant.service;

import com.example.aistudyassistant.entity.ChatSession;
import com.example.aistudyassistant.mapper.ChatSessionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatSessionService {
    private final ChatSessionMapper chatSessionMapper;
    // 通过构造方法接收并保存所需依赖或初始化字段。
    public ChatSessionService(ChatSessionMapper chatSessionMapper) {
        this.chatSessionMapper = chatSessionMapper;
    }
    // 构造并保存会话后返回生成的 ID。
    public Long insertChatSession(String title, Long userId) {
        ChatSession session = new ChatSession();
        session.setTitle(title);
        session.setUserId(userId);

        chatSessionMapper.insertChatSession(session);
        return session.getId();
    }
    // 查询指定用户自己的会话列表。
    public List<ChatSession> findChatSessionByUserId(Long userId){
        return chatSessionMapper.findChatSessionByUserId(userId);
    }
    // 根据记录 ID 和用户 ID 联合查找，限制数据归属。
    public ChatSession findByIdAndUserId(Long id, Long userId) {
        return chatSessionMapper.findByIdAndUserId(id, userId);
    }
}
