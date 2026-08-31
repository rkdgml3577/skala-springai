package com.example.demo.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ai.chat.client.ChatClient;

@RestController
class HelloAiController {
    private final ChatClient chat;

    HelloAiController(ChatClient.Builder builder) {
        this.chat = builder.build(); // 자동 구성된 빌더를 주입받는다
    }

    @GetMapping("/hello")
    String hello(@RequestParam(defaultValue = "안녕하세요") String q) {
        return chat.prompt().user(q).call().content();
    }
}