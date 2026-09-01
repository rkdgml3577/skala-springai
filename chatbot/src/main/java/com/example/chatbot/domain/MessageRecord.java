package com.example.chatbot.domain;

import org.springframework.ai.chat.messages.Message;

/**
 * 저장된 메시지 한 건을 밖으로 내보내는 형태.
 *
 * Spring AI 의 Message 를 그대로 응답에 실으면 내부 구조가 API 에 새어 나간다.
 * 화면이 필요한 것은 "누가" 와 "무슨 말" 둘뿐이다.
 */
public record MessageRecord(String role, String text) {

    public static MessageRecord from(Message message) {
        return new MessageRecord(
                message.getMessageType().getValue(),
                message.getText() == null ? "" : message.getText());
    }
}
