package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ai.chat.client.ChatClient;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // 자동 구성된 빌더를 주입받는다
    @GetMapping
    public String chat(@RequestParam String message) {
        return chatClient
                .prompt() // ① 요청 구성 시작
                .user(message) // ② 사용자 메시지
                .call() // ③ 동기 호출
                .content(); // ④ 문자열로 받기
    }
}