package com.shixiaoyu.xiangyueproject.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.shixiaoyu.xiangyueproject.constants.CommonConstants.*;

@Configuration
public class AIConfiguration {
    @Bean
    public ChatClient villageClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem(VILLAGE_AI_PROMPT)
                .build();
    }

    @Bean
    public ChatClient scenicClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem(SCENIC_AI_PROMPT)
                .build();
    }

    @Bean
    public ChatClient specialityClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem(SPECIALTY_AI_PROMPT)
                .build();
    }

    @Bean
    public ChatClient commonChatClient(OpenAiChatModel openAiChatModel, ChatMemory chatMemory) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem(COMMON_AI_PROMPT)
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory,"default",10))
                .build();
    }

    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    @Bean
    public ChatClient multiClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem(MULTI_AI_PROMPT)
                .build();
    }
}
