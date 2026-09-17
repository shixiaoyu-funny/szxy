package com.shixiaoyu.xiangyueproject.service;

import com.shixiaoyu.xiangyueproject.entity.dto.AiChatDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.AnonymousChatDTO;
import com.shixiaoyu.xiangyueproject.entity.po.ChatMessage;
import com.shixiaoyu.xiangyueproject.entity.po.ChatSession;

import java.util.List;

public interface AIService {
    String chat(Long id, AiChatDTO dto);

    List<ChatMessage> getChatsBySid(Long id);

    List<ChatSession> getSessionsByUid();

    Long newSession();

    /** 修改当前用户名下会话的侧栏标题 */
    void renameSession(Long sessionId, String simpleDesc);

    /** 游客匿名聊天（不落库、不持久化，上下文由前端 history 传入） */
    String chatAnonymous(AnonymousChatDTO dto);
}
