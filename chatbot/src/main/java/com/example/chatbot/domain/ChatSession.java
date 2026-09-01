package com.example.chatbot.domain;

import java.util.List;

/**
 * 대화 세션 한 개의 요약.
 *
 * 목록 화면에 전체 대화를 다 실어 보낼 이유는 없다.
 * 어떤 세션이 있고, 얼마나 오갔고, 마지막에 무슨 말이 있었는지면 충분하다.
 */
public record ChatSession(String sessionId, int messageCount, String lastMessage) {

    private static final int PREVIEW_LENGTH = 40;

    /**
     * 저장소에 실제로 쓰이는 키.
     *
     * 사용자를 앞에 붙인다. 이게 없으면 alice 와 bob 이 우연히 같은 세션 이름을
     * 쓰는 순간 서로의 대화가 섞인다 — 로그인은 됐는데 남의 말이 보이는 상태다.
     */
    public static String conversationId(String userId, String sessionId) {
        return userId + ":" + sessionId;
    }

    /** 내 것만 골라내기 위한 접두사. */
    public static String prefixOf(String userId) {
        return userId + ":";
    }

    /** 저장소 키에서 사용자 부분을 떼고 사용자가 지은 이름만 돌려준다. */
    public static String sessionIdOf(String conversationId, String userId) {
        return conversationId.substring(prefixOf(userId).length());
    }

    public static ChatSession of(String sessionId, List<MessageRecord> messages) {
        String preview = messages.isEmpty()
                ? ""
                : trim(messages.get(messages.size() - 1).text());
        return new ChatSession(sessionId, messages.size(), preview);
    }

    private static String trim(String text) {
        String oneLine = text.replaceAll("\\s+", " ").strip();
        return oneLine.length() <= PREVIEW_LENGTH
                ? oneLine
                : oneLine.substring(0, PREVIEW_LENGTH) + "…";
    }
}
