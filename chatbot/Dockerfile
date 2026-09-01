# ── 1단계 : 빌드 ────────────────────────────────────────────────
# 빌드 도구(JDK·Gradle)는 여기서만 쓰고 최종 이미지에는 넣지 않는다.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 목록만 먼저 복사해 받아 둔다.
# 소스만 바뀐 다음 빌드에서는 이 층이 캐시에 그대로 남아 다시 받지 않는다.
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon

# ── 2단계 : 실행 ────────────────────────────────────────────────
# JDK 가 아니라 JRE 다. 컴파일러가 들어 있을 이유가 없다.
FROM eclipse-temurin:21-jre
WORKDIR /app

# root 로 돌리지 않는다.
RUN useradd -r -u 1001 spring
USER spring

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
# 한글 로그와 프롬프트가 깨지지 않게 인코딩을 고정한다.
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "/app/app.jar"]
