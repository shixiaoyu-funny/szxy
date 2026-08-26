package com.shixiaoyu.xiangyueproject.consumer;

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
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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

    /**
     * 处理AI聊天相关存储落库逻辑
     */
    @RabbitHandler
    public void solveChatMemory(Map<String, String> map) {
        long sessionId = Long.parseLong(map.get("sid"));
        long userId = Long.parseLong(map.get("uid"));
        String content = map.get("content");
        String aiRes = map.get("aiRes");
        //生成redis键
        String key = AI_CHAT_SESSION_PREFIX + sessionId;
        //如果是第一次对话，需要修改会话简述
        //根据会话id查出该会话是否存在，存在则判断是否是第一次
        LambdaQueryWrapper<ChatMessage> chatMessageLambdaQueryWrapper = new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSessionId, sessionId);
        List<ChatMessage> chatMessage = chatMessageMapper.selectList(chatMessageLambdaQueryWrapper);
        String summary;
        if (chatMessage.isEmpty()) {
            summary = qwenFlashClient
                    .prompt()
                    .system(SUMMARIES_MESSAGES)
                    .user("用户问题：" + content + "\nAI回答：" + aiRes)
                    .call()
                    .content();
            LambdaUpdateWrapper<ChatSession> chatSessionLambdaUpdateWrapper = new LambdaUpdateWrapper<ChatSession>()
                    .eq(ChatSession::getId, sessionId)
                    .eq(ChatSession::getUserId, userId)
                    .set(ChatSession::getSimpleDesc, summary);
            chatSessionMapper.update(chatSessionLambdaUpdateWrapper);
            //是第一次对话就需要在redis里面插入一条问候语
            ChatMessage helloMessage = messageSetting(ChatMessageRoleEnum.ASSISTANT, sessionId, userId, AI_HELLO);
            stringRedisTemplate.opsForList().rightPush(key,JSONUtil.toJsonStr(helloMessage));
            stringRedisTemplate.expire(key,AI_CHAT_SESSION_TTL, TimeUnit.HOURS);
        }

        //存库
        ChatMessage userMessage = messageSetting(ChatMessageRoleEnum.USER, sessionId, userId, content);
        chatMessageMapper.insert(userMessage);
        ChatMessage aiMessage = messageSetting(ChatMessageRoleEnum.ASSISTANT, sessionId, userId, aiRes);
        chatMessageMapper.insert(aiMessage);
        //更新会话时间
        LambdaUpdateWrapper<ChatSession> sessionWrapper = new LambdaUpdateWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getId, sessionId).set(ChatSession::getUpdateTime, LocalDateTime.now());
        chatSessionMapper.update(sessionWrapper);
        //更新redis数据
        stringRedisTemplate.opsForList().rightPushAll(key, JSONUtil.toJsonStr(userMessage), JSONUtil.toJsonStr(aiMessage));
        stringRedisTemplate.expire(key,AI_CHAT_SESSION_TTL, TimeUnit.HOURS);
    }

    //配置聊天消息
    private ChatMessage messageSetting(ChatMessageRoleEnum roleEnum, Long sessionId, Long userId, String content) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setContent(content);
        chatMessage.setRole(roleEnum);
        chatMessage.setSessionId(sessionId);
        chatMessage.setUserId(userId);
        return chatMessage;
    }
}
