package com.example.aistudyassistant.mapper;

import com.example.aistudyassistant.entity.ChatMessage;
import com.example.aistudyassistant.entity.ChatSession;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatSessionMapper {
    @Insert("""
        INSERT INTO chat_sessions (title, user_id)
        VALUES (#{title}, #{userId})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    // 插入会话并将生成的 ID 回填到会话对象。
    int insertChatSession(ChatSession session);

    @Select("""
        SELECT id, title, user_id AS userId, created_at AS createdAt FROM chat_sessions
        WHERE user_id = #{userId}
        """)
    // 查询指定用户自己的会话列表。
    List<ChatSession> findChatSessionByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT id, title, user_id AS userId, created_at AS createdAt FROM chat_sessions
        WHERE id = #{id} AND user_id = #{userId}
        """)
    // 根据记录 ID 和用户 ID 联合查找，限制数据归属。
    ChatSession findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    // 保存流式对话前锁定会话并再次校验归属。
    @Select("SELECT id FROM chat_sessions WHERE id = #{id} AND user_id = #{userId} FOR UPDATE")
    Long lockOwnedSessionId(@Param("id") Long id, @Param("userId") Long userId);

}
