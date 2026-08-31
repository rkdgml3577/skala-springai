package com.skala.minutes.minutes;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 여기가 오늘의 작업장이다. TODO ① ② ③ 세 군데를 채운다.
 *
 * 프롬프트 파일은 이미 src/main/resources/prompts 에 들어 있다 — 새로 쓸 필요 없다.
 * 필요한 것은 전부 이 파일 안에 있다. 다른 파일은 STEP 4 에서 컨트롤러 하나만 건드린다.
 *
 * 확인 방법 : ./gradlew test
 * STEP 을 하나 끝낼 때마다 STEP 1 → 2 → 3 순서로 초록이 늘어난다.
 */
@Service
public class MinutesService {


    private final ChatClient chat;
    private final int maxSentences;

    @Value("classpath:/prompts/summary.st")
    private Resource summaryPrompt;

    @Value("classpath:/prompts/report.st")
    private Resource reportPrompt;

    public MinutesService(ChatClient chat, @Value("${app.max-sentences}") int maxSentences) {
        this.chat = chat;
        this.maxSentences = maxSentences;
    }

    // ══ STEP 1 ═══════════════════════════════════════════════════════════
    /**
     * TODO ① 회의록을 한 문단으로 요약해서 돌려준다.
     *
     * 할 일
     * 1. summaryPrompt 파일을 system 으로 넘긴다.
     * 2. 템플릿 안의 {maxSentences} 자리를 maxSentences 값으로 채운다.
     * 3. 회의록 본문을 user 로 넘긴다.
     * 4. content() 가 아니라 chatResponse() 로 받는다. 토큰 수를 보려면 응답 전체가 필요하다.
     * 5. 아래 logUsage("summary", 응답) 를 부른 뒤, 본문 텍스트를 돌려준다.
     *
     * 뼈대
     * ChatResponse response = chat.prompt()
     * .system(s -> s.text(...).param("이름", 값))
     * .user(...)
     * .call()
     * .chatResponse();
     *
     * 본문 꺼내기 : response.getResult().getOutput().getText()
     *
     * 통과 기준 : STEP 1 테스트 4개가 초록이 된다.
     */
    public String summarize(String minutes) {
        ChatResponse response = chat.prompt()
                .system(s -> s.text(summaryPrompt)
                        .param("maxSentences", maxSentences))
                .user(minutes)
                .call()
                .chatResponse();

        return response.getResult().getOutput().getText();
    }

    // public String summarize(String minutes) { // 회의록을 받아서 → 요약을 돌려준다

    // ChatResponse response = chat.prompt() // "AI한테 말 걸 준비"
    // .system(s -> s.text(summaryPrompt) // 지침서 = summary.st 파일
    // .param("maxSentences", 3)) // 지침서 빈칸에 3 채우기
    // .user(minutes) // 서류 = 받은 회의록
    // .call() // 엔터! (여기서 기다림)
    // .chatResponse(); // 답장 상자를 통째로 받기

    // logUsage("summary", response); // 영수증을 로그에 남기고
    // return response.getResult() // 상자에서
    // .getOutput()
    // .getText(); // 본문만 꺼내서 돌려준다
    // }

    // ══ STEP 2 ═══════════════════════════════════════════════════════════
    /**
     * TODO ② 결과를 MeetingReport 객체로 바로 받는다.
     *
     * STEP 1 과 거의 같다. 다른 점은 두 가지뿐이다.
     * - 프롬프트가 summaryPrompt 가 아니라 reportPrompt 다. (param 은 필요 없다)
     * - 마지막 한 줄이 chatResponse() 가 아니라 entity(MeetingReport.class) 다.
     *
     * MeetingReport 는 이미 완성되어 있다. 그 필드 이름이 그대로 모델에게 주는 지시가 된다.
     *
     * 실행해 보면 필드가 비어서 올 수 있다. 그때 고칠 곳은 이 코드가 아니라
     * prompts/report.st 다. 이게 오늘 실습에서 제일 중요한 경험이다.
     *
     * 통과 기준 : STEP 2 테스트 3개가 초록이 된다.
     */
    public MeetingReport report(String minutes) {
        return chat.prompt()
                .system(s -> s.text(reportPrompt))
                .user(minutes)
                .call()
                .entity(MeetingReport.class);
    }

    // ══ STEP 3 ═══════════════════════════════════════════════════════════
    /**
     * TODO ③ 같은 요약을 스트리밍으로 내보낸다.
     *
     * STEP 1 과 프롬프트가 똑같다. 딱 두 군데만 바꾼다.
     * - call() → stream()
     * - chatResponse() → content()
     *
     * 재시도는 걸지 않는다. 이미 흘려보낸 글자는 되돌릴 수 없기 때문이다.
     *
     * 통과 기준 : STEP 3 테스트 2개가 초록이 된다.
     */
    public Flux<String> streamSummary(String minutes) {
        return chat.prompt()
                .system(s -> s.text(summaryPrompt)
                        .param("maxSentences", maxSentences))
                .user(minutes)
                .stream()
                .content();
    }

    // ══ STEP 5 (선택) ════════════════════════════════════════════════════
    // 여기까지 오면 오늘 목표를 넘긴 것이다. 시간이 남을 때만 한다.
    //
    // summarize() 와 report() 에 재시도를 건다.
    //
    // @Retryable(retryFor = TransientAiException.class,
    // maxAttempts = 3,
    // backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000))
    //
    // 세 번 모두 실패했을 때 돌려줄 값은 @Recover 메서드에 적는다.
    // @Recover 의 첫 인자는 예외, 나머지는 원래 메서드와 같은 인자여야 한다.
    //
    // @Recover
    // public String recoverSummarize(TransientAiException e, String minutes) { ...
    // }
    //
    // 확인) OPENAI_API_KEY 를 일부러 틀리게 넣고 호출해 본다.
    // 재시도가 도는지, 그 뒤 어떤 응답이 나가는지 로그로 본다.

}
