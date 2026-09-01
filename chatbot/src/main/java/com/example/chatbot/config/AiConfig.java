package com.example.chatbot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;

/**
 * 공통 설정을 빈 하나에 모은다.
 *
 * 시스템 프롬프트도, 대화 메모리도, 토큰 기록도 전부 여기서 붙인다.
 * 그래서 호출하는 쪽(ChatService)에는 프롬프트와 옵션이 한 줄도 남지 않는다.
 *
 * 공급자는 코드가 아니라 설정으로 고른다 — application.yml 의 app.provider 한 줄이다.
 */
@Configuration
public class AiConfig {

    /**
     * 시스템 프롬프트는 자바 코드에 문자열로 박지 않는다.
     * 프롬프트를 고치는 일과 코드를 고치는 일은 서로 다른 일이기 때문이다.
     */
    @Value("classpath:/prompts/system.st")
    private Resource systemPrompt;

    /**
     * 대화 메모리.
     *
     * 창(window)의 크기만 여기서 정하고, "어디에 담을지" 는 밖에서 받는다.
     * 이 한 줄(chatMemoryRepository)이 인메모리와 Redis 를 가르는 전부다 —
     * ChatService 도 컨트롤러도 저장소가 무엇인지 모른다.
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository,
                                 @Value("${app.max-messages}") int maxMessages) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(maxMessages)      // 최근 N 건만 유지한다
                .build();
    }

    @Bean
    public ChatClient chatClient(
            @Value("${app.provider}") String provider,
            @Value("${app.temperature}") double temperature,
            @Qualifier("openAiChatModel") ChatModel openAiModel,
            @Qualifier("ollamaChatModel") ChatModel ollamaModel,
            ChatMemory chatMemory) {

        ChatModel model = "ollama".equalsIgnoreCase(provider) ? ollamaModel : openAiModel;

        return ChatClient.builder(model)
                .defaultSystem(systemPrompt, StandardCharsets.UTF_8)
                .defaultOptions(ChatOptions.builder()
                        .temperature(temperature)
                        .build())
                // 순서가 중요하다. 메모리 어드바이저가 바깥(먼저)에서 이전 대화를 붙이고,
                // 토큰 어드바이저는 안쪽(나중)에서 모델이 돌려준 응답을 본다.
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new TokenUsageAdvisor())
                .build();
    }
}
