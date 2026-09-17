package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shixiaoyu.xiangyueproject.common.tools.CommonTool;
import com.shixiaoyu.xiangyueproject.common.tools.OrderAndCartTool;
import com.shixiaoyu.xiangyueproject.common.tools.ScVgQueryTool;
import com.shixiaoyu.xiangyueproject.common.tools.WebSearchTool;
import com.shixiaoyu.xiangyueproject.entity.dto.AiChatDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.AnonymousChatDTO;
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
import org.springframework.ai.content.Media;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.shixiaoyu.xiangyueproject.constants.CommonConstants.CHAT_ROUTING_KEY;
import static com.shixiaoyu.xiangyueproject.constants.CommonConstants.EXCHANGE_NAME;
import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.AI_CHAT_SESSION_PREFIX;
import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.AI_CHAT_SESSION_TTL;

@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {
    private static final int MAX_MEDIA = 6;

    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatClient qwenFlashClient;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final CommonTool commonTool;
    private final ScVgQueryTool scVgQueryTool;
    private final WebSearchTool webSearchTool;
    private final OrderAndCartTool orderAndCartTool;

    /**
     * 聊天业务（支持文本 + 图片 URL 多模态，见 Spring AI Multimodality）
     */
    @Override
    public String chat(Long sessionId, AiChatDTO dto) {
        String content = dto == null ? null : StrUtil.trim(dto.getContent());
        List<String> mediaUrls = normalizeMediaUrls(dto == null ? null : dto.getMediaUrls());
        if (StrUtil.isBlank(content) && CollUtil.isEmpty(mediaUrls)) {
            throw new BusinessException("请输入文字或上传图片");
        }
        if (StrUtil.isBlank(content)) {
            content = "请结合图片回答我的问题。";
        }
        //上下文压缩（保留最近20条）
        List<ChatMessage> msgs = getMsgs(sessionId);
        List<Message> history = msgs.stream().map(this::toSpringMessage).toList();
        int start = Math.max(0, history.size() - 20);
        List<Message> messages = new ArrayList<>(history.subList(start, history.size()));
        // 文本 + Media(URI) 多模态 UserMessage
        messages.add(buildUserMessage(content + "\nchatTime:" + LocalDateTime.now(), mediaUrls));

        String aiRes = qwenFlashClient
                .prompt()
                .messages(messages)
                .tools(commonTool, scVgQueryTool, webSearchTool, orderAndCartTool)
                .call()
                .content();
        //mq处理存储逻辑
        Map<String, String> map = new HashMap<>();
        map.put("sid", sessionId.toString());
        map.put("uid", UserHolder.getUser().getId().toString());
        map.put("content", content);
        map.put("mediaUrls", JSONUtil.toJsonStr(mediaUrls));
        map.put("aiRes", aiRes);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, CHAT_ROUTING_KEY, map);
        return aiRes;
    }

    @Override
    public List<ChatMessage> getChatsBySid(Long sessionId) {
        return getMsgs(sessionId);
    }

    @Override
    public List<ChatSession> getSessionsByUid() {
        Long userId = UserHolder.getUser().getId();
        return chatSessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime));
    }

    @Override
    public String chatAnonymous(AnonymousChatDTO dto) {
        String content = dto == null ? null : StrUtil.trim(dto.getContent());
        List<String> mediaUrls = normalizeMediaUrls(dto == null ? null : dto.getMediaUrls());
        if (StrUtil.isBlank(content) && CollUtil.isEmpty(mediaUrls)) {
            throw new BusinessException("请输入文字或上传图片");
        }
        if (StrUtil.isBlank(content)) {
            content = "请结合图片回答我的问题。";
        }
        // 用前端传入的历史构造上下文（不依赖登录态、不落库）
        List<Message> messages = new ArrayList<>();
        List<AnonymousChatDTO.HistoryMessage> history = dto == null ? null : dto.getHistory();
        if (CollUtil.isNotEmpty(history)) {
            for (AnonymousChatDTO.HistoryMessage h : history) {
                String hc = StrUtil.blankToDefault(h.getContent(), "");
                if (h.getRole() != null && h.getRole() == 1) {
                    messages.add(buildUserMessage(hc, normalizeMediaUrls(h.getMediaUrls())));
                } else {
                    messages.add(new AssistantMessage(hc));
                }
            }
        }
        messages.add(buildUserMessage(content + "\nchatTime:" + LocalDateTime.now(), mediaUrls));
        // 匿名聊天不注册 OrderAndCartTool（依赖登录用户），仅提供查询类工具
        return qwenFlashClient
                .prompt()
                .messages(messages)
                .tools(commonTool, scVgQueryTool, webSearchTool)
                .call()
                .content();
    }

    @Override
    public Long newSession() {
        Long userId = UserHolder.getUser().getId();
        List<ChatSession> chatSessions = chatSessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getSimpleDesc, "新对话"));
        if (!chatSessions.isEmpty()) {
            throw new BusinessException(500, "新会话已创建成功，请勿重复创建");
        }
        ChatSession chatSession = new ChatSession();
        chatSession.setUserId(userId);
        chatSessionMapper.insert(chatSession);
        return chatSession.getId();
    }

    @Override
    public void renameSession(Long sessionId, String simpleDesc) {
        if (sessionId == null) {
            throw new BusinessException("会话ID不能为空");
        }
        String name = StrUtil.trim(simpleDesc);
        if (StrUtil.isBlank(name)) {
            throw new BusinessException("会话名称不能为空");
        }
        if (name.length() > 64) {
            throw new BusinessException("会话名称最多 64 个字符");
        }
        Long userId = UserHolder.getUser().getId();
        ChatSession session = chatSessionMapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getId, sessionId)
                .eq(ChatSession::getUserId, userId));
        if (session == null) {
            throw new BusinessException(500, "该对话不存在");
        }
        chatSessionMapper.update(new LambdaUpdateWrapper<ChatSession>()
                .eq(ChatSession::getId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .set(ChatSession::getSimpleDesc, name)
                .set(ChatSession::getUpdateTime, LocalDateTime.now()));
    }

    private List<ChatMessage> getMsgs(Long sessionId) {
        Long userId = UserHolder.getUser().getId();
        //查询会话是否存在
        String key = AI_CHAT_SESSION_PREFIX + sessionId;
        ChatSession session = chatSessionMapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getId, sessionId)
                .eq(ChatSession::getUserId, userId));
        if (session == null) {
            throw new BusinessException(500, "该对话不存在");
        }
        //查询缓存
        List<String> cache = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (cache != null && !cache.isEmpty()) {
            return cache.stream().map(s -> JSONUtil.toBean(s, ChatMessage.class)).toList();
        }
        //会话存在无缓存，进行缓存重建
        List<ChatMessage> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getUserId, userId)
                .orderByAsc(ChatMessage::getCreateTime));
        if (!messages.isEmpty()) {
            String[] jsons = messages.stream().map(JSONUtil::toJsonStr).toArray(String[]::new);
            stringRedisTemplate.opsForList().rightPushAll(key, jsons);
            stringRedisTemplate.expire(key, AI_CHAT_SESSION_TTL, TimeUnit.HOURS);
        }
        return messages;
    }

    /** 历史/当前消息 → Spring AI Message（用户侧可带 Media） */
    private Message toSpringMessage(ChatMessage msg) {
        if (msg.getRole() != null && msg.getRole().getCode() == 1) {
            return buildUserMessage(StrUtil.blankToDefault(msg.getContent(), ""),
                    normalizeMediaUrls(msg.getMediaUrls()));
        }
        return new AssistantMessage(StrUtil.blankToDefault(msg.getContent(), ""));
    }

    /**
     * 构建多模态 UserMessage：
     * text → content；图片以公网 URL 作为 Media(URI) 传入（OpenAI 兼容协议）。
     *
     * @see <a href="https://docs.spring.io/spring-ai/reference/1.1/api/multimodality.html">Spring AI Multimodality</a>
     */
    private UserMessage buildUserMessage(String text, List<String> mediaUrls) {
        UserMessage.Builder builder = UserMessage.builder().text(text);
        List<Media> mediaList = toMediaList(mediaUrls);
        if (!mediaList.isEmpty()) {
            builder.media(mediaList);
        }
        return builder.build();
    }

    private List<Media> toMediaList(List<String> mediaUrls) {
        if (CollUtil.isEmpty(mediaUrls)) {
            return List.of();
        }
        List<Media> list = new ArrayList<>();
        for (String url : mediaUrls) {
            try {
                // 公网 URL 以 URI 形式交给 Spring AI（OpenAI 兼容 image_url）
                list.add(new Media(resolveImageMime(url), URI.create(url)));
            } catch (Exception e) {
                throw new BusinessException("无效的图片地址：" + url);
            }
        }
        return list;
    }

    private List<String> normalizeMediaUrls(List<String> raw) {
        if (CollUtil.isEmpty(raw)) {
            return List.of();
        }
        List<String> urls = raw.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .filter(u -> u.startsWith("http://") || u.startsWith("https://"))
                .distinct()
                .collect(Collectors.toList());
        if (urls.size() > MAX_MEDIA) {
            throw new BusinessException("一次最多上传 " + MAX_MEDIA + " 张图片");
        }
        return urls;
    }

    private MimeType resolveImageMime(String url) {
        String path = url;
        int q = url.indexOf('?');
        if (q >= 0) {
            path = url.substring(0, q);
        }
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) {
            return MimeTypeUtils.IMAGE_PNG;
        }
        if (lower.endsWith(".gif")) {
            return MimeTypeUtils.IMAGE_GIF;
        }
        if (lower.endsWith(".webp")) {
            return MimeType.valueOf("image/webp");
        }
        if (lower.endsWith(".bmp")) {
            return MimeType.valueOf("image/bmp");
        }
        return MimeTypeUtils.IMAGE_JPEG;
    }
}
