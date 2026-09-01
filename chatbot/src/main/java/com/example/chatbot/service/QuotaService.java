package com.example.chatbot.service;

import com.example.chatbot.web.QuotaExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 사용자별 하루 사용량을 센다.
 *
 * 지금은 프로세스 메모리에 있다 — 앱을 내리면 카운터가 0 으로 돌아가고,
 * 인스턴스를 늘리면 각자 따로 센다. 운영에서는 Redis 의 INCR + EXPIRE 로 바꾼다.
 * 그때 바뀌는 것은 이 클래스 안쪽뿐이고, 부르는 쪽은 그대로다.
 */
@Service
public class QuotaService {

    private final int dailyQuota;

    private final Map<String, AtomicInteger> used = new ConcurrentHashMap<>();
    private LocalDate countedOn = LocalDate.now();

    public QuotaService(@Value("${app.daily-quota}") int dailyQuota) {
        this.dailyQuota = dailyQuota;
    }

    /**
     * 한 건을 쓴 것으로 치고 통과시킨다. 한도를 넘었으면 던진다.
     *
     * 이것을 스트림 안이 아니라 앞에서 불러야 한다.
     * 이미 조각이 흐르기 시작한 뒤에는 되돌릴 수 없기 때문이다.
     */
    public void checkAndDecrease(String userId) {
        rollOverIfNewDay();

        int count = used.computeIfAbsent(userId, k -> new AtomicInteger())
                .incrementAndGet();

        if (count > dailyQuota) {
            throw new QuotaExceededException(
                    "일일 사용량을 초과했다. 하루 %d건까지 쓸 수 있다".formatted(dailyQuota));
        }
    }

    /** 남은 횟수. 화면에 보여 주거나 테스트에서 확인할 때 쓴다. */
    public int remaining(String userId) {
        rollOverIfNewDay();
        AtomicInteger count = used.get(userId);
        return Math.max(0, dailyQuota - (count == null ? 0 : count.get()));
    }

    /** 날짜가 바뀌면 전부 0 으로 되돌린다. */
    private synchronized void rollOverIfNewDay() {
        LocalDate today = LocalDate.now();
        if (!today.equals(countedOn)) {
            used.clear();
            countedOn = today;
        }
    }
}
