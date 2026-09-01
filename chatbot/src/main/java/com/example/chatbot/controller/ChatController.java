package com.example.chatbot.controller;

import com.example.chatbot.domain.ChatRequest;
import com.example.chatbot.service.ChatService;
import com.example.chatbot.service.QuotaService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 스트리밍 엔드포인트.
 *
 * 컨트롤러는 얇게 둔다 — 받고, 막고, 이벤트로 감싸는 것까지다.
 * 모델 호출과 계측은 ChatService 에 있다.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    /**
     * 1단계에서는 사용자가 한 명이라고 친다.
     * 2단계에서 Spring Security 를 붙이면 @AuthenticationPrincipal UserDetails 로 바뀌고,
     * 그 순간 세션과 사용량이 사용자별로 갈라진다.
     */
    private static final String CURRENT_USER = "dev";

    private final ChatService chatService;
    private final QuotaService quotaService;

    public ChatController(ChatService chatService, QuotaService quotaService) {
        this.chatService = chatService;
        this.quotaService = quotaService;
    }

    /** 모델을 부르지 않는다. 뼈대가 떴는지만 본다. */
    @GetMapping("/ping")
    public String ping() {
        return "chatbot 준비됨 — 남은 사용량 " + quotaService.remaining(CURRENT_USER);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ChatRequest request) {

        // 사용량 확인은 스트림 안이 아니라 여기, 스트림이 만들어지기 전에 한다.
        // 이미 흐르기 시작한 뒤에는 되돌릴 수 없고, HTTP 상태도 바꿀 수 없다.
        // 여기서 던진 QuotaExceededException 은 ApiExceptionHandler 가 429 로 받는다.
        quotaService.checkAndDecrease(CURRENT_USER);

        return chatService
                .streamChat(CURRENT_USER, request.sessionIdOrDefault(), request.message())
                .map(chunk -> ServerSentEvent.builder(chunk).event("message").build())
                // 끝났다는 사실을 따로 알린다. 이게 없으면 화면은
                // "답이 끝난 것"과 "연결이 끊긴 것"을 구분할 수 없다.
                .concatWith(Mono.just(ServerSentEvent.<String>builder()
                        .event("done").data("[DONE]").build()))
                // 스트림 도중의 실패. 응답은 이미 200 으로 나갔으므로
                // 상태 코드가 아니라 이벤트로 알리는 수밖에 없다.
                .onErrorResume(e -> Mono.just(ServerSentEvent.<String>builder()
                        .event("error").data(e.getMessage()).build()));
    }
}
