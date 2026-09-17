package com.shixiaoyu.xiangyueproject.common.config;

import com.shixiaoyu.xiangyueproject.entity.dto.UserDTO;
import com.shixiaoyu.xiangyueproject.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static com.shixiaoyu.xiangyueproject.constants.PromptConstants.*;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AIConfiguration {
    private final ChatClient.Builder builder;
    /**
     * 默认 qwen3.8-flash（原生多模态：文本 + 图片/视频理解）。
     */
    @Bean
    @Primary
    public ChatClient qwenFlashClient() {
        return builder
                .defaultSystem(fullSystemPrompt())
                .build();
    }

//    @Bean
//    public ChatClient qwenPlusClient() {
//        return builder
//                .defaultSystem(fullSystemPrompt())
//                .build();
//    }

    /**
     * AI工具日志打印方法
     * @param toolName
     */
    public static void logToolInvoked(String toolName) {
        UserDTO user = UserHolder.getUser();
        log.info("{}工具被调用，调用用户id：{},用户名：{}", toolName, user.getId(), user.getUsername());
    }
}
