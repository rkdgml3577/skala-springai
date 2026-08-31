package com.skala.minutes.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 공통 설정을 빈 하나에 모은다.
 *
 * 스타터를 둘 다 넣었기 때문에 ChatModel 빈이 공급자별로 만들어진다.
 * 어느 것을 쓸지는 application.yml 의 app.provider 한 줄이 정한다 —
 * 서비스와 컨트롤러 코드는 공급자를 전혀 모른다.
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(
            @Value("${app.provider}") String provider,
            @Value("${app.temperature}") double temperature,
            @Qualifier("openAiChatModel") ChatModel openAiModel,
            @Qualifier("ollamaChatModel") ChatModel ollamaModel) {

        ChatModel model = "ollama".equalsIgnoreCase(provider) ? ollamaModel : openAiModel;

        return ChatClient.builder(model)
                .defaultOptions(ChatOptions.builder()
                        .temperature(temperature)
                        .build())
                .build();
    }
}
