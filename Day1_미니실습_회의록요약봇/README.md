# Day 1 미니 실습 — 회의록 요약봇

회의록을 넣으면 요약과 할 일을 뽑아 주는 REST API 를 만든다.
PART 1~2 에서 다룬 것만 사용한다 — RAG 와 도구는 나오지 않는다.

- 시간 : 90분 (단계마다 20분 정도)
- 목표선 : STEP 4 까지. 재시도(선택)는 시간이 남으면 한다.
- 폴더 : `starter/` 안에서 작업한다.
- 막히면 : 먼저 `./gradlew test` 의 실패 메시지를 읽는다. 무엇이 어긋났는지 그 안에 있다.
  그래도 안 풀리면 강사에게 묻는다. 정답 코드는 실습이 끝난 뒤에 따로 배포한다.

> :warning: **`starter/` 는 테스트 12개가 실패한 상태로 배포된다. 고장이 아니라 출발선이다.**
> STEP 을 하나 끝낼 때마다 초록으로 바뀐다. (3절 참고)

---

## 0. 전체 그림 (강의자료 117번)

강의자료 **117번 「회의록 요약봇의 구조」** 한 장이 이 실습의 전부다. 띄워 놓고 시작한다.

```
브라우저
  └▶ MinutesController        /api/minutes/summary · report · stream
       └▶ MinutesService      프롬프트 조립 · 재시도 · 토큰 기록
            └▶ ChatClient
                 └▶ OpenAI · Ollama

브라우저 ◀── SSE 조각으로 되돌아온다 (STEP 3 에서 만든다)
```

- TODO 여섯 군데는 전부 가운데 두 칸(`MinutesService` · `MinutesController`)에 있다.
- 양쪽 끝(브라우저 · 모델)은 손대지 않는다. **공급자는 코드가 아니라 설정으로 바꾼다.**
- 되돌아오는 점선이 STEP 3 에서 만들 스트리밍 경로다.

---

## 1. 준비

```bash
# ① OpenAI 로 할 때 — 키를 환경변수로 넣는다
export OPENAI_API_KEY=sk-...
cd starter
./gradlew bootRun

# ② 키 없이 Ollama 로 할 때
ollama pull qwen3.5:2b
cd starter
./gradlew bootRun --args='--spring.profiles.active=local'
```

뜨고 나서 이것부터 확인한다. 모델을 부르지 않으므로 키가 없어도 응답이 온다.

```bash
curl localhost:8080/api/minutes/ping
# meeting-minutes 준비됨
```

브라우저로 `http://localhost:8080` 를 열면 붙여 넣고 눌러 보는 화면이 있다.
회의록 예시는 `src/main/resources/samples/minutes-sample.txt` 에 들어 있다.

## 2. 채울 곳

`starter` 안의 **TODO 네 군데**다. 순서대로 간다. 각 STEP 은 앞 STEP 과 독립이라
막히면 건너뛰어도 다음 것이 돌아간다.

| STEP | 파일 | TODO | 하는 일 |
| --- | --- | --- | --- |
| 1 | `minutes/MinutesService.java` | ① `summarize()` | 프롬프트 파일 + 설정값으로 요약을 만든다 |
| 2 | `minutes/MinutesService.java` | ② `report()` | `entity()` 로 객체를 바로 받는다 |
| 3 | `minutes/MinutesService.java` | ③ `streamSummary()` | `call()` 을 `stream()` 으로 바꾼다 |
| 4 | `web/MinutesController.java` | ④ `stream()` | `done` · `error` 이벤트를 붙인다 |
| 선택 | `minutes/MinutesService.java` | — | 재시도(`@Retryable` · `@Recover`) |

`MeetingReport` 와 프롬프트 파일(`prompts/summary.st`, `report.st`)은 **이미 완성되어 있다.**
손대지 않는다 — 단, 필드가 비어서 올 때는 코드가 아니라 프롬프트를 고친다.

## 3. 다 됐는지 확인하기

### 테스트로 확인한다 — 이게 기본이다

**`starter` 는 테스트 12개가 빨간 상태로 출발한다. 고장이 아니라 출발선이다.**
STEP 을 하나 끝낼 때마다 빨간 줄이 줄어든다.

```bash
./gradlew test
```

```
▸ STEP 1 · 요약을 만든다              4개
▸ STEP 2 · 구조화 출력으로 받는다        3개
▸ STEP 3 · 스트리밍으로 내보낸다         2개
▸ STEP 4 · 완료와 오류를 이벤트로 구분한다  3개
```

한 단계만 돌려 볼 수도 있다.

```bash
./gradlew test --tests '*StepTests$STEP1*'
```

이 테스트들은 **모델도 키도 네트워크도 쓰지 않는다.** 가짜 모델을 끼우고
`ChatClient` 체인은 진짜 그대로 돌리기 때문에, 프롬프트를 제대로 조립했는지까지 확인된다.
예를 들어 STEP 1 은 `{maxSentences}` 자리가 설정값 `3` 으로 치환됐는지를 본다.

처음 4개(`뼈대가_뜬다` 등)는 시작부터 초록이다. 뼈대가 살아 있다는 뜻이다.

### 실제 모델로도 확인한다

테스트가 전부 초록이 된 뒤, 진짜 모델에 붙여 본다.

```bash
# STEP 1
curl -X POST localhost:8080/api/minutes/summary \
     -H 'Content-Type: application/json' \
     -d "{\"text\": \"$(cat src/main/resources/samples/minutes-sample.txt | tr '\n' ' ')\"}"

# STEP 2 — 필드가 다 찼는지 본다. 비어 있으면 prompts/report.st 를 고친다
curl -X POST localhost:8080/api/minutes/report \
     -H 'Content-Type: application/json' \
     -d '{"text":"김지훈: 타임아웃을 10초로 줄인다. 이도현이 수요일까지 반영한다."}'

# STEP 3·4 — -N 을 빠뜨리면 다 모아서 한 번에 찍힌다
curl -N -X POST localhost:8080/api/minutes/stream \
     -H 'Content-Type: application/json' \
     -d '{"text":"김지훈: 타임아웃을 10초로 줄인다."}'
```

IntelliJ · VS Code 를 쓰면 `http/minutes.http` 를 열어 그대로 눌러도 된다.
브라우저 `http://localhost:8080` 에서도 붙여 넣고 눌러 볼 수 있다.

### 아직 안 채운 곳을 부르면

`UnsupportedOperationException: TODO ① summarize() 를 채운다` 가 나온다.
"돌아가는 척" 하지 않으므로 어디가 남았는지 바로 보인다.

## 4. 제출 전 체크리스트

- [ ] `./gradlew test` 가 전부 초록이다
- [ ] 프롬프트가 자바 코드 안에 문자열로 박혀 있지 않다
- [ ] 구조화 출력 필드가 비어 오면 **프롬프트**를 고쳐서 채웠다
- [ ] 스트리밍이 완료(`done`)와 오류(`error`)를 각각 이벤트로 내보낸다
- [ ] 키를 틀리게 넣었을 때 500 대신 무슨 일인지 알 수 있는 메시지가 나온다
- [ ] 요청 한 건의 입력·출력 토큰이 로그에 남는다

## 5. 더 해 볼 것

- `app.temperature` 를 `0.2` 와 `1.0` 으로 두고 같은 회의록을 세 번씩 돌려 본다.
  요약이 얼마나 흔들리는지 눈으로 확인한다.
- `app.provider` 를 `ollama` 로 바꿔 본다. **자바 코드는 한 줄도 고치지 않는다.**
- `prompts/report.st` 에서 "지어내지 않는다" 줄을 지우고 돌려 본다.
  없는 담당자가 생기는지 본다.
