package com.shixiaoyu.xiangyueproject.service;

import com.shixiaoyu.xiangyueproject.entity.po.ChatMessage;
import com.shixiaoyu.xiangyueproject.entity.po.ChatSession;

import java.util.List;

public interface AIService {
    String chat(Long id, String content);

    List<ChatMessage> getChatsBySid(Long id);

    List<ChatSession> getSessionsByUid();

    Long newSession();
}
