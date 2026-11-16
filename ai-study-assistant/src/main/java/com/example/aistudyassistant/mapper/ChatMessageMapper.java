package com.example.aistudyassistant.mapper;

import com.example.aistudyassistant.entity.ChatMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Insert("""
            INSERT INTO chat_messages (session_id, role, content)
            VALUES (#{sessionId}, #{role}, #{content})
            """)
    // 将一条会话消息插入数据库。
    int insertChatMessage(ChatMessage message);

    @Select("""
            SELECT id, session_id AS sessionId, role, content,
                   created_at AS createdAt
            FROM chat_messages
            WHERE session_id = #{sessionId}
            ORDER BY id ASC
            """)
    // 按消息 ID 升序读取会话全部消息。
    List<ChatMessage> findChatMessagesBySessionId(
            @Param("sessionId") Long sessionId
    );

    @Select("""
            SELECT id, session_id AS sessionId, role, content,
                   created_at AS createdAt
            FROM chat_messages
            WHERE session_id = #{sessionId}
            ORDER BY id DESC LIMIT #{limit}
            """)
    // 按消息 ID 降序读取指定数量的近期消息。
    List<ChatMessage> findRecentMessagesBySessionId(
            @Param("sessionId") Long sessionId,
            @Param("limit") int limit
    );
}