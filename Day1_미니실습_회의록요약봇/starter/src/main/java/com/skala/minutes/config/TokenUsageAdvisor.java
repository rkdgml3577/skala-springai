package com.skala.minutes.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 모델 호출의 토큰 사용량을 한 곳에서 기록한다.
 *
 * 토큰 기록은 "요약을 만든다", "리포트를 만든다" 와 아무 상관이 없는 관심사다.
 * 그래서 서비스 메서드마다 흩어 놓지 않고 여기 한 곳에 모은다.
 * ChatClient 를 거치는 모든 호출이 자동으로 지나가므로, 새 메서드를 만들어도
 * 기록을 빠뜨릴 수 없다.
 *
 * call() 과 stream() 은 경로가 다르므로 두 인터페이스를 모두 구현한다.
 */
public class TokenUsageAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageAdvisor.class);

    @Override
    public String getName() {
        return "tokenUsage";
    }

    /** 체인의 맨 안쪽에 둔다 — 모델이 돌려준 응답을 가장 먼저 본다. */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    // ── 한 번에 받는 호출 ──────────────────────────────────────────
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);   // 실제 모델 호출
        log("call", usageOf(response.chatResponse()));
        return response;                                         // 받은 그대로 흘려보낸다
    }

    // ── 스트리밍 호출 ────────────────────────────────────────────
    // 토큰 정보는 마지막 조각에만 실려 오므로, 흘려보내면서 붙잡아 두었다가
    // 스트림이 끝날 때 한 번만 기록한다.
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        AtomicReference<Usage> lastSeen = new AtomicReference<>();

        return chain.nextStream(request)
                .doOnNext(response -> {
                    Usage usage = usageOf(response.chatResponse());
                    if (usage != null) {
                        lastSeen.set(usage);
                    }
                })
                .doOnComplete(() -> log("stream", lastSeen.get()));
    }

    // ── 공통 ────────────────────────────────────────────────────
    private static Usage usageOf(ChatResponse response) {
        if (response == null || response.getMetadata() == null) {
            return null;
        }
        Usage usage = response.getMetadata().getUsage();
        // 스트리밍 중간 조각은 0 으로 채워져 오기도 한다
        return (usage == null || usage.getTotalTokens() == null || usage.getTotalTokens() == 0)
                ? null
                : usage;
    }

    private static void log(String kind, Usage usage) {
        if (usage == null) {                 // Ollama 는 안 채워 줄 때가 있다
            log.debug("[{}] 토큰 정보 없음", kind);
            return;
        }
        log.info("[{}] 토큰 입력 {} 출력 {} 합계 {}", kind,
                usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }
}
