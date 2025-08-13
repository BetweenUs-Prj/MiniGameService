# =========================
# 1) Builder (Gradle)
# =========================
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Windows에서 체크아웃한 경우 gradlew에 CRLF가 있어 실행 실패 방지
COPY gradlew ./
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Gradle 캐시 살리기 위해 먼저 의존성 단계만 복사/해결
COPY gradle ./gradle
COPY settings.gradle build.gradle ./
RUN ./gradlew --no-daemon dependencies || true

# 소스 복사 후 빌드 (테스트 제외)
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

# =========================
# 2) Runtime (JRE)
# =========================
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# non-root 사용자
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 타임존/로케일(옵션)
ENV TZ=Asia/Seoul \
    LANG=ko_KR.UTF-8 \
    LANGUAGE=ko_KR:ko:en_US:en \
    LC_ALL=ko_KR.UTF-8

# 빌드 산출물 복사
COPY --from=builder /app/build/libs/*.jar /app/app.jar

# 실행 환경 변수 (필요에 맞게 compose에서 override)
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8" \
    SPRING_PROFILES_ACTIVE=dev \
    SERVER_PORT=8080
EXPOSE 8080

# Actuator 헬스체크 (있으면 통과)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:${SERVER_PORT}/actuator/health || exit 1

USER appuser
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --server.port=${SERVER_PORT}"]
