package com.shixiaoyu.xiangyueproject.consumer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shixiaoyu.xiangyueproject.entity.po.ChatMessage;
import com.shixiaoyu.xiangyueproject.entity.po.ChatSession;
import com.shixiaoyu.xiangyueproject.enums.ChatMessageRoleEnum;
import com.shixiaoyu.xiangyueproject.mapper.ChatMessageMapper;
import com.shixiaoyu.xiangyueproject.mapper.ChatSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.shixiaoyu.xiangyueproject.constants.CommonConstants.*;
import static com.shixiaoyu.xiangyueproject.constants.PromptConstants.AI_HELLO;
import static com.shixiaoyu.xiangyueproject.constants.PromptConstants.SUMMARIES_MESSAGES;
import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.AI_CHAT_SESSION_PREFIX;
import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.AI_CHAT_SESSION_TTL;

/**
 * AI聊天消费者
 */
@Slf4j
@Component
@RabbitListener(
        bindings = @QueueBinding(value = @Queue(name = CHAT_QUEUE_NAME),
                exchange = @Exchange(name = EXCHANGE_NAME),
                key = CHAT_ROUTING_KEY)
)
@RequiredArgsConstructor
public class ChatQueueConsumer {
    private final StringRedisTemplate stringRedisTemplate;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatClient qwenFlashClient;
    private final TransactionTemplate transactionTemplate;

    @RabbitHandler
    public void solveChatMemory(Map<String, String> map) {
        long sessionId = Long.parseLong(map.get("sid"));
        long userId = Long.parseLong(map.get("uid"));
        String content = map.get("content");
        String aiRes = map.get("aiRes");
        List<String> mediaUrls = parseMediaUrls(map.get("mediaUrls"));
        String key = AI_CHAT_SESSION_PREFIX + sessionId;

        List<ChatMessage> exists = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSessionId, sessionId));
        final String sessionSummary;
        if (exists.isEmpty()) {
            String userPart = content;
            if (mediaUrls != null && !mediaUrls.isEmpty()) {
                userPart = content + "（附带" + mediaUrls.size() + "张图片）";
            }
            sessionSummary = qwenFlashClient
                    .prompt()
                    .system(SUMMARIES_MESSAGES)
                    .user("用户问题：" + userPart + "\nAI回答：" + aiRes)
                    .call()
                    .content();
            ChatMessage helloMessage = messageSetting(ChatMessageRoleEnum.ASSISTANT, sessionId, userId, AI_HELLO, null);
            stringRedisTemplate.opsForList().rightPush(key, JSONUtil.toJsonStr(helloMessage));
            stringRedisTemplate.expire(key, AI_CHAT_SESSION_TTL, TimeUnit.HOURS);
        } else {
            sessionSummary = null;
        }

        List<ChatMessage> saved = transactionTemplate.execute(status -> {
            if (StrUtil.isNotBlank(sessionSummary)) {
                chatSessionMapper.update(new LambdaUpdateWrapper<ChatSession>()
                        .eq(ChatSession::getId, sessionId)
                        .eq(ChatSession::getUserId, userId)
                        .set(ChatSession::getSimpleDesc, sessionSummary));
            }
            ChatMessage userMessage = messageSetting(ChatMessageRoleEnum.USER, sessionId, userId, content, mediaUrls);
            chatMessageMapper.insert(userMessage);
            ChatMessage aiMessage = messageSetting(ChatMessageRoleEnum.ASSISTANT, sessionId, userId, aiRes, null);
            chatMessageMapper.insert(aiMessage);
            chatSessionMapper.update(new LambdaUpdateWrapper<ChatSession>()
                    .eq(ChatSession::getUserId, userId)
                    .eq(ChatSession::getId, sessionId)
                    .set(ChatSession::getUpdateTime, LocalDateTime.now()));
            return List.of(userMessage, aiMessage);
        });

        if (saved != null) {
            stringRedisTemplate.opsForList().rightPushAll(key,
                    JSONUtil.toJsonStr(saved.get(0)), JSONUtil.toJsonStr(saved.get(1)));
            stringRedisTemplate.expire(key, AI_CHAT_SESSION_TTL, TimeUnit.HOURS);
        }
    }

    private List<String> parseMediaUrls(String json) {
        if (StrUtil.isBlank(json) || "null".equals(json) || "[]".equals(json)) {
            return Collections.emptyList();
        }
        try {
            return JSONUtil.toList(json, String.class);
        } catch (Exception e) {
            log.warn("解析 mediaUrls 失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    private ChatMessage messageSetting(ChatMessageRoleEnum roleEnum, Long sessionId, Long userId,
                                       String content, List<String> mediaUrls) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setContent(content);
        chatMessage.setRole(roleEnum);
        chatMessage.setSessionId(sessionId);
        chatMessage.setUserId(userId);
        if (mediaUrls != null && !mediaUrls.isEmpty()) {
            chatMessage.setMediaUrls(mediaUrls);
        }
        return chatMessage;
    }
}
