package com.skala.minutes.minutes;

import java.util.List;

/**
 * 구조화 출력으로 받을 모양. 이건 이미 완성되어 있다 — 손대지 않는다.
 *
 * 중요한 것은 이 record 가 "모델에게 주는 지시" 라는 점이다.
 * 필드 이름(title · summary · decisions · actionItems)이 그대로 JSON 키가 되고,
 * 모델은 그 이름을 보고 무엇을 채울지 판단한다. 필드 이름이 곧 프롬프트다.
 *
 * dueDate 가 LocalDate 가 아니라 String 인 이유:
 *   모델은 "수요일까지", "다음 주 금요일" 같은 말을 그대로 돌려준다.
 *   LocalDate 로 두면 이런 답에서 변환이 깨져 전체 응답이 실패한다.
 *   날짜로 바꾸는 일은 받아 온 뒤에 우리 코드가 한다.
 */
public record MeetingReport(
        String title,
        String summary,
        List<String> decisions,
        List<ActionItem> actionItems) {

    public record ActionItem(String owner, String task, String dueDate) {
    }
}
