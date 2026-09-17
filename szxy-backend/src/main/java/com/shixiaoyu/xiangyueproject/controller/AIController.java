package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.dto.AiChatDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.AnonymousChatDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.ChatSessionRenameDTO;
import com.shixiaoyu.xiangyueproject.entity.po.ChatMessage;
import com.shixiaoyu.xiangyueproject.entity.po.ChatSession;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI")
@Slf4j
public class AIController {

    private final AIService aiService;

    /**
     * AI 会话（支持文本 + 图片 URL 多模态）
     */
    @PostMapping("/chat/{id}")
    @Operation(summary = "发送消息（可附带图片URL）")
    public Result<String> chat(@PathVariable Long id, @RequestBody AiChatDTO dto) {
        return Result.ok(aiService.chat(id, dto));
    }

    @PostMapping("/chat/anonymous")
    @Operation(summary = "游客匿名聊天（不落库、不持久化）")
    public Result<String> chatAnonymous(@RequestBody AnonymousChatDTO dto) {
        return Result.ok(aiService.chatAnonymous(dto));
    }

    @GetMapping("/gcBySID/{id}")
    public Result<List<ChatMessage>> getChatsBySid(@PathVariable Long id) {
        return Result.ok(aiService.getChatsBySid(id));
    }

    @GetMapping("/gssByUID")
    public Result<List<ChatSession>> getSessionsByUid() {
        return Result.ok(aiService.getSessionsByUid());
    }

    @PostMapping("/new")
    public Result<Long> newSession() {
        return Result.ok(aiService.newSession());
    }

    @PostMapping("/rename/{id}")
    @Operation(summary = "修改会话名称")
    public Result<Void> renameSession(@PathVariable Long id, @RequestBody ChatSessionRenameDTO dto) {
        aiService.renameSession(id, dto == null ? null : dto.getSimpleDesc());
        return Result.ok();
    }
}
