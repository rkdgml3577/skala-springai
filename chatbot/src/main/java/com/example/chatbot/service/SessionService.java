package com.example.chatbot.service;

import com.example.chatbot.domain.ChatSession;
import com.example.chatbot.domain.MessageRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 세션을 들여다보고 지우는 일을 맡는다.
 *
 * 여기가 아는 것은 ChatMemoryRepository 인터페이스뿐이다 —
 * 이력이 힙에 있는지 Redis 에 있는지 이 클래스는 모르고, 알 필요도 없다.
 * 저장소를 바꿔도 이 파일은 그대로다.
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final ChatMemoryRepository repository;

    public SessionService(ChatMemoryRepository repository) {
        this.repository = repository;
    }

    /**
     * 내 세션 목록.
     *
     * 저장소는 모든 사용자의 키를 다 돌려준다. 그중 내 접두사로 시작하는 것만 남긴다.
     * 이 걸러내기를 빼먹으면 로그인은 됐는데 남의 대화 제목이 보이게 된다.
     */
    public List<ChatSession> findSessions(String userId) {
        String prefix = ChatSession.prefixOf(userId);
        List<ChatSession> sessions = new ArrayList<>();
        for (String conversationId : repository.findConversationIds()) {
            if (!conversationId.startsWith(prefix)) {
                continue;                       // 남의 대화다
            }
            String sessionId = ChatSession.sessionIdOf(conversationId, userId);
            sessions.add(ChatSession.of(sessionId, findHistory(userId, sessionId)));
        }
        sessions.sort(Comparator.comparing(ChatSession::sessionId));
        return sessions;
    }

    /**
     * 세션 하나의 대화 전체.
     *
     * sessionId 는 사용자가 보낸 값이다. 그대로 저장소 키로 쓰면
     * 남의 세션 이름을 넣어 훔쳐볼 수 있다. 반드시 내 접두사를 붙여서 찾는다.
     */
    public List<MessageRecord> findHistory(String userId, String sessionId) {
        List<Message> messages =
                repository.findByConversationId(ChatSession.conversationId(userId, sessionId));
        List<MessageRecord> records = new ArrayList<>(messages.size());
        for (Message m : messages) {
            records.add(MessageRecord.from(m));
        }
        return records;
    }

    /** 대화를 지운다. 다음 질문부터는 처음 만난 사이가 된다. */
    public void deleteSession(String userId, String sessionId) {
        repository.deleteByConversationId(ChatSession.conversationId(userId, sessionId));
        log.info("세션을 지웠다: userId={}, sessionId={}", userId, sessionId);
    }
}
