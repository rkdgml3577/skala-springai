package com.example.chatbot.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 대화 이력을 Redis 에 둔다.
 *
 * Spring AI 1.1.8 에는 Redis 용 ChatMemoryRepository 가 없다 (2.0.0 부터 생겼다).
 * 그래서 직접 만든다 — 채울 것은 네 메서드뿐이다.
 *
 * 이 클래스가 생기면 대화 이력은 프로세스 밖으로 나간다.
 *   - 앱을 내렸다 올려도 대화가 이어진다
 *   - 인스턴스를 여러 개 띄워도 어느 쪽에 붙든 같은 기억을 본다
 *
 * 바뀌는 것은 AiConfig 의 한 줄뿐이고, ChatService 와 컨트롤러는 그대로다.
 */
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final String KEY_PREFIX = "chat:memory:";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Duration ttl;

    public RedisChatMemoryRepository(StringRedisTemplate redis, ObjectMapper mapper, Duration ttl) {
        this.redis = redis;
        this.mapper = mapper;
        this.ttl = ttl;
    }

    @Override
    public List<String> findConversationIds() {
        List<String> ids = new ArrayList<>();
        // KEYS 는 서버를 통째로 멈춰 세운다. 커서로 조금씩 훑는다.
        try (Cursor<String> cursor = redis.scan(
                ScanOptions.scanOptions().match(KEY_PREFIX + "*").count(100).build())) {
            cursor.forEachRemaining(key -> ids.add(key.substring(KEY_PREFIX.length())));
        }
        return ids;
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<String> stored = redis.opsForList().range(key(conversationId), 0, -1);
        if (stored == null || stored.isEmpty()) {
            return List.of();
        }
        List<Message> messages = new ArrayList<>(stored.size());
        for (String json : stored) {
            messages.add(toMessage(json));
        }
        return messages;
    }

    /**
     * 창(window)을 다시 계산한 전체 목록이 통째로 넘어온다.
     * 그래서 덧붙이는 게 아니라 통째로 갈아 끼운다.
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = key(conversationId);
        redis.delete(key);
        if (messages.isEmpty()) {
            return;
        }
        List<String> payload = new ArrayList<>(messages.size());
        for (Message m : messages) {
            payload.add(toJson(m));
        }
        redis.opsForList().rightPushAll(key, payload);
        // TTL 을 걸지 않으면 한 번 쓰고 버려진 세션이 영원히 남는다.
        redis.expire(key, ttl);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redis.delete(key(conversationId));
    }

    private static String key(String conversationId) {
        return KEY_PREFIX + conversationId;
    }

    // ── 직렬화 ──────────────────────────────────────────────────
    // Message 를 그대로 Jackson 에 맡기지 않는다. 구현체가 여러 개라
    // 되읽을 때 어느 타입이었는지 알 수 없기 때문이다.
    // 종류와 본문 두 가지만 적어 두고, 읽을 때 다시 조립한다.

    private String toJson(Message message) {
        try {
            return mapper.writeValueAsString(
                    java.util.Map.of("type", message.getMessageType().getValue(),
                                     "text", message.getText() == null ? "" : message.getText()));
        } catch (Exception e) {
            throw new IllegalStateException("대화 이력을 저장하지 못했다", e);
        }
    }

    private Message toMessage(String json) {
        try {
            var map = mapper.readValue(json, new TypeReference<java.util.Map<String, String>>() {});
            String type = map.getOrDefault("type", "user");
            String text = map.getOrDefault("text", "");
            return switch (type) {
                case "assistant" -> new AssistantMessage(text);
                case "system" -> new SystemMessage(text);
                default -> new UserMessage(text);
            };
        } catch (Exception e) {
            throw new IllegalStateException("대화 이력을 읽지 못했다", e);
        }
    }
}
