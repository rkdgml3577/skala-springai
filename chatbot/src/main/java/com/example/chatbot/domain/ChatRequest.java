package com.example.chatbot.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 대화 한 마디.
 *
 * sessionId 가 대화를 가르는 유일한 기준이다 — 같은 값이면 맥락이 이어지고,
 * 다른 값이면 서로를 모른다. 비워서 보내면 "default" 로 본다.
 */
public record ChatRequest(

        String sessionId,

        @NotBlank(message = "메시지가 비어 있다")
        @Size(max = 4000, message = "메시지가 너무 길다. 4000자 이내로 보낸다")
        String message) {

    public String sessionIdOrDefault() {
        return (sessionId == null || sessionId.isBlank()) ? "default" : sessionId;
    }
}
