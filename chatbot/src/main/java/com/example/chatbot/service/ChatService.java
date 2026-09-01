package com.example.chatbot.service;

import com.example.chatbot.domain.ChatSession;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 모델 호출과 계측을 한 곳에 둔다.
 *
 * 프롬프트도 옵션도 여기 없다 — 전부 AiConfig 의 ChatClient 빈에 붙어 있다.
 * 이 클래스가 하는 일은 "어느 대화인지 알려 주고, 얼마나 걸렸는지 재는 것" 둘뿐이다.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final MeterRegistry meterRegistry;

    public ChatService(ChatClient chatClient, MeterRegistry meterRegistry) {
        this.chatClient = chatClient;
        this.meterRegistry = meterRegistry;
    }

    public Flux<String> streamChat(String userId, String sessionId, String message) {
        // defer 로 감싼다. Flux 는 구독될 때 비로소 흐르므로,
        // 시계도 그때 시작해야 실제 응답 시간을 잰다.
        return Flux.defer(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);

            return chatClient.prompt()
                    .user(message)
                    // 이 한 줄이 대화를 가른다. 같은 사용자의 같은 sessionId 면
                    // 이전 맥락이 함께 실리고, 사용자가 다르면 키부터 달라진다.
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID,
                            ChatSession.conversationId(userId, sessionId)))
                    .stream()
                    .content()
                    .doOnComplete(() -> {
                        record(sample, userId, "success");
                        log.info("응답 완료: userId={}, sessionId={}", userId, sessionId);
                    })
                    .doOnError(e -> {
                        record(sample, userId, "error");
                        log.error("응답 오류: userId={}, sessionId={}, {}",
                                userId, sessionId, e.getMessage());
                    })
                    // 사용자가 떠나도 스트림은 계속 흐른다 — 그동안 비용은 계속 나간다.
                    // doOnCancel 을 걸어야 그 지점에서 호출이 끊기고, 끊긴 사실이 기록된다.
                    .doOnCancel(() -> {
                        record(sample, userId, "cancelled");
                        log.warn("응답 취소: userId={}, sessionId={} — 사용자가 연결을 끊었다",
                                userId, sessionId);
                    });
        });
    }

    /**
     * user_id 를 태그로 달면 사용자 수만큼 시계열이 늘어난다.
     * 실습 규모에서는 사용자별 사용 패턴을 보는 값이 더 크지만,
     * 사용자가 수천 명이 되는 순간 이 줄부터 지워야 한다.
     */
    private void record(Timer.Sample sample, String userId, String outcome) {
        sample.stop(Timer.builder("chatbot.response.duration")
                .tag("user_id", userId)
                .tag("outcome", outcome)
                .register(meterRegistry));
    }
}
