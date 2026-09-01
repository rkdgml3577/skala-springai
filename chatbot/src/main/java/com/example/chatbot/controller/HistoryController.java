package com.example.chatbot.controller;

import com.example.chatbot.domain.ChatSession;
import com.example.chatbot.domain.MessageRecord;
import com.example.chatbot.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 쌓인 대화를 들여다보는 창.
 *
 * 스트리밍과 달리 여기는 모델을 부르지 않는다 — 저장소만 읽는다.
 * 그래서 사용량도 세지 않고, 응답도 한 번에 나간다.
 */
@RestController
@RequestMapping("/api/chat/sessions")
public class HistoryController {

    private final SessionService sessionService;

    public HistoryController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /** 내 세션 목록. 남의 것은 나오지 않는다. */
    @GetMapping
    public List<ChatSession> sessions(@AuthenticationPrincipal UserDetails user) {
        return sessionService.findSessions(user.getUsername());
    }

    /**
     * 세션 하나의 대화 전체.
     *
     * sessionId 를 경로로 받지만 그대로 저장소에 넘기지 않는다.
     * 로그인한 사람의 이름을 앞에 붙여 찾으므로, 남의 세션 이름을 넣어도 빈 목록이 나온다.
     */
    @GetMapping("/{sessionId}")
    public List<MessageRecord> history(@AuthenticationPrincipal UserDetails user,
                                       @PathVariable String sessionId) {
        return sessionService.findHistory(user.getUsername(), sessionId);
    }

    /** 내 대화를 지운다. 되돌릴 수 없으므로 본문 없이 204 로 끝낸다. */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails user,
                                       @PathVariable String sessionId) {
        sessionService.deleteSession(user.getUsername(), sessionId);
        return ResponseEntity.noContent().build();
    }
}
