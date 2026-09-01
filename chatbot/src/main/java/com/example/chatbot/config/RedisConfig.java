package com.example.chatbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 대화 이력을 어디에 둘지 정하는 자리.
 *
 * 이 빈 하나가 있으면 Spring AI 의 기본 저장소(InMemoryChatMemoryRepository)가
 * 자동설정에서 빠지고, 이력이 Redis 로 나간다.
 */
@Configuration
public class RedisConfig {

    @Bean
    public ChatMemoryRepository chatMemoryRepository(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.memory-ttl}") Duration memoryTtl) {
        return new RedisChatMemoryRepository(redisTemplate, objectMapper, memoryTtl);
    }
}
