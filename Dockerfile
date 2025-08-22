# =========================
# 1) Builder (Gradle)
# =========================
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Windows CRLF 대비
COPY gradlew ./
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# 의존성 캐시
COPY gradle ./gradle
COPY settings.gradle build.gradle ./
RUN ./gradlew --no-daemon dependencies || true

# 소스 빌드
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

# =========================
# 2) Runtime (JRE)
# =========================
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# non-root
RUN groupadd -r appuser && useradd -r -g appuser appuser

# curl + ko_KR.UTF-8 로케일
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl locales && \
    sed -i 's/^# *\(ko_KR.UTF-8\)/\1/' /etc/locale.gen && locale-gen && \
    rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Seoul \
    LANG=ko_KR.UTF-8 \
    LANGUAGE=ko_KR:ko:en_US:en \
    LC_ALL=ko_KR.UTF-8

# 빌드 산출물
COPY --from=builder /app/build/libs/*.jar /app/app.jar

# 런타임 옵션
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8" \
    SPRING_PROFILES_ACTIVE=dev \
    SERVER_PORT=8080
EXPOSE 8080

# Actuator 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:${SERVER_PORT}/actuator/health || exit 1

USER appuser
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --server.port=${SERVER_PORT}"]
