package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shixiaoyu.xiangyueproject.common.tools.CommonTool;
import com.shixiaoyu.xiangyueproject.common.tools.ScienceAndVillageTool;
import com.shixiaoyu.xiangyueproject.common.tools.WebSearchTool;
import com.shixiaoyu.xiangyueproject.entity.po.ChatMessage;
import com.shixiaoyu.xiangyueproject.entity.po.ChatSession;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.mapper.ChatMessageMapper;
import com.shixiaoyu.xiangyueproject.mapper.ChatSessionMapper;
import com.shixiaoyu.xiangyueproject.service.AIService;
import com.shixiaoyu.xiangyueproject.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static com.shixiaoyu.xiangyueproject.constants.CommonConstants.CHAT_ROUTING_KEY;
import static com.shixiaoyu.xiangyueproject.constants.CommonConstants.EXCHANGE_NAME;
import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.AI_CHAT_SESSION_PREFIX;
import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.AI_CHAT_SESSION_TTL;

@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {
    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatClient qwenFlashClient;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final CommonTool commonTool;
    private final ScienceAndVillageTool scienceAndVillageTool;
    private final WebSearchTool webSearchTool;



    /**
     * 聊天业务
     *
     * @param sessionId
     * @param content
     */
    @Override
    public String chat(Long sessionId, String content) {
        List<ChatMessage> msgs = getMsgs(sessionId);
        List<Message> messagesWaitToSolve = msgs.stream().map(msg -> msg.getRole().getCode() == 1
                        ? (Message) new UserMessage(msg.getContent())
                        : new AssistantMessage(msg.getContent()))
                .toList();
        //对话截取
        int start = Math.max(0, messagesWaitToSolve.size() - 20);
        List<Message> messages = new ArrayList<>(messagesWaitToSolve.subList(start, messagesWaitToSolve.size()));
        //获取ai生成结果
        String aiRes = qwenFlashClient
                .prompt()
                .messages(messages)
                .user("content:" + content + "\nchatTime:" + LocalDateTime.now())
                .tools(commonTool, scienceAndVillageTool,webSearchTool)
                .call()
                .content();
        //放到mq异步执行+异常处理
        Map<String, String> map = new HashMap<>();
        map.put("sid", sessionId.toString());
        map.put("uid", UserHolder.getUser().getId().toString());
        map.put("content", content);
        map.put("aiRes", aiRes);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, CHAT_ROUTING_KEY, map);
        return aiRes;
    }

    /**
     * 根据会话id获取历史聊天记录
     *
     * @param sessionId
     * @return
     */
    public List<ChatMessage> getChatsBySid(Long sessionId) {
        return getMsgs(sessionId);
    }

    /**
     * 根据用户id获取用户历史会话集合
     *
     * @return
     */
    @Override
    public List<ChatSession> getSessionsByUid() {
        Long userId = UserHolder.getUser().getId();
        LambdaQueryWrapper<ChatSession> chatSessionLambdaQueryWrapper = new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime);
        return chatSessionMapper.selectList(chatSessionLambdaQueryWrapper);
    }

    /**
     * 新建会话
     */
    @Override
    public Long newSession() {
        LambdaQueryWrapper<ChatSession> chatSessionLambdaQueryWrapper = new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, UserHolder.getUser().getId())
                .eq(ChatSession::getSimpleDesc, "新对话");
        List<ChatSession> chatSessions = chatSessionMapper.selectList(chatSessionLambdaQueryWrapper);
        if (!chatSessions.isEmpty()) {
            throw new BusinessException(500, "新会话已创建成功，请勿重复创建");
        }
        Long userId = UserHolder.getUser().getId();
        ChatSession chatSession = new ChatSession();
        chatSession.setUserId(userId);
        chatSessionMapper.insert(chatSession);

        return chatSession.getId();
    }

    //根据会话id获取聊天记录
    private List<ChatMessage> getMsgs(Long sessionId) {
        Long userId = UserHolder.getUser().getId();
        String key = AI_CHAT_SESSION_PREFIX + sessionId;
        // 1. 先校验会话归属（命中/未命中都要）
        ChatSession session = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getId, sessionId)
                        .eq(ChatSession::getUserId, userId)
        );
        if (session == null) {
            throw new BusinessException(500, "该对话不存在");
        }
        // 2. 读缓存
        List<String> cache = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (cache != null && !cache.isEmpty()) {
            return cache.stream()
                    .map(s -> JSONUtil.toBean(s, ChatMessage.class))
                    .toList();
        }
        // 3. 重建缓存：RPUSH 保持 ASC；可加 LTRIM / expire
        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getUserId, userId)
                        .orderByAsc(ChatMessage::getCreateTime)
        );
        if (!messages.isEmpty()) {
            String[] jsons = messages.stream()
                    .map(JSONUtil::toJsonStr)
                    .toArray(String[]::new);
            stringRedisTemplate.opsForList().rightPushAll(key, jsons);
            stringRedisTemplate.expire(key, AI_CHAT_SESSION_TTL, TimeUnit.HOURS);
        }
        return messages;
    }
}
