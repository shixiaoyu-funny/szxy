package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.po.ChatMessage;
import com.shixiaoyu.xiangyueproject.entity.po.ChatSession;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.service.AIService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name="AI")
@Slf4j
public class AIController {

    private final AIService aiService;

    /**
     * AI会话接口
     * @param id
     * @param content
     * @return
     */
    @PostMapping("/chat/{id}")
    public Result<String> chat(
            @PathVariable Long id,
            @RequestParam String content
    ){
        String res=aiService.chat(id,content);
        return Result.ok(res);
    }

    /**
     * 根据会话id获取历史聊天记录
     * @param id
     * @return
     */
    @GetMapping("/gcBySID/{id}")
    public Result<List<ChatMessage>> getChatsBySid(
            @PathVariable Long id
    ){
        List<ChatMessage> chatInfos=aiService.getChatsBySid(id);
        return Result.ok(chatInfos);
    }

    /**
     * 根据用户id获取用户历史会话集合
     * @return
     */
    @GetMapping("/gssByUID")
    public Result<List<ChatSession>> getSessionsByUid(){
        List<ChatSession> sessions=aiService.getSessionsByUid();
        return Result.ok(sessions);
    }

    /**
     * 新建会话
     */
    @PostMapping("/new")
    public Result<Long> newSession(){
        Long sid = aiService.newSession();
        return Result.ok(sid);
    }
}
