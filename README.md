# Mini Game Service

Spring Boot 3.4.0 기반의 미니게임 서비스입니다.

## 기술 스택

- **Java**: 17
- **Spring Boot**: 3.4.0
- **Gradle**: 8.8
- **Database**: H2 (개발용), MySQL, PostgreSQL
- **Cache**: Redis
- **Security**: Spring Security + JWT
- **WebSocket**: 실시간 통신
- **Build Tool**: Gradle

## 포함된 의존성

### Spring Boot Starters
- `spring-boot-starter-web`: 웹 애플리케이션 개발
- `spring-boot-starter-data-jpa`: JPA 데이터 액세스
- `spring-boot-starter-security`: 보안 기능
- `spring-boot-starter-validation`: 데이터 검증
- `spring-boot-starter-actuator`: 모니터링 및 관리
- `spring-boot-starter-data-redis`: Redis 캐시
- `spring-boot-starter-websocket`: WebSocket 지원

### Database Drivers
- `h2`: 개발용 인메모리 데이터베이스
- `mysql-connector-j`: MySQL 데이터베이스
- `postgresql`: PostgreSQL 데이터베이스

### 기타 라이브러리
- `lombok`: 보일러플레이트 코드 제거
- `jjwt`: JWT 토큰 처리
- `spring-boot-starter-test`: 테스트 지원

## 실행 방법

### 1. 애플리케이션 실행
```bash
# Gradle Wrapper 사용
./gradlew bootRun

# 또는 빌드 후 실행
./gradlew build
java -jar build/libs/minigame-service-0.0.1-SNAPSHOT.jar
```

### 2. 접속 정보
- **애플리케이션**: http://localhost:8080
- **H2 Console**: http://localhost:8080/h2-console
- **Actuator**: http://localhost:8080/actuator
- **API Health Check**: http://localhost:8080/api/games/health

### 3. H2 Database 접속 정보
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **Username**: `sa`
- **Password**: (비어있음)

## 프로젝트 구조

```
src/
├── main/
│   ├── java/
│   │   └── com/minigame/service/
│   │       ├── MiniGameServiceApplication.java
│   │       └── controller/
│   │           └── GameController.java
│   └── resources/
│       └── application.yml
└── test/
    └── java/
        └── com/minigame/service/
```

## 설정 파일

`application.yml`에서 다음 설정들을 확인할 수 있습니다:

- **서버 포트**: 8080
- **데이터베이스**: H2 인메모리
- **Redis**: localhost:6379
- **JWT**: 설정 가능
- **Actuator**: health, info, metrics 엔드포인트 활성화

## 개발 환경 설정

1. Java 17 이상 설치
2. IDE에서 프로젝트 열기
3. Gradle 동기화
4. 애플리케이션 실행

## 빌드 및 배포

```bash
# 빌드
./gradlew build

# 테스트 실행
./gradlew test

# JAR 파일 생성
./gradlew bootJar
```

## API 엔드포인트

- `GET /api/games/health`: 서비스 상태 확인
- `GET /api/games/`: 환영 메시지

## 🎯 주요 특징

### 게임 플로우
1. **세션 생성**: 호스트가 게임 타입과 카테고리를 선택하여 세션 생성
2. **참가자 모집**: 초대 코드로 친구들 초대 또는 공개 방에서 참여
3. **게임 시작**: 모든 참가자가 준비되면 호스트가 게임 시작
4. **라운드 진행**: 문제별/클릭별로 실시간 경쟁
5. **결과 확인**: 점수 집계 후 순위 및 벌칙 대상 발표

### 확장성 고려사항
- **멱등성**: 중복 요청 방지로 안정적인 게임 진행
- **에러 핸들링**: 4xx/5xx 에러에 대한 명확한 응답
- **동시성**: 멀티플레이어 환경에서의 데이터 일관성
- **성능**: Connection Pool과 인덱스 최적화

## 🔄 데이터베이스 마이그레이션

```bash
# Flyway 마이그레이션 상태 확인
./gradlew flywayInfo

# 수동 마이그레이션 실행
./gradlew flywayMigrate
```

## 🚀 배포 및 운영

이 서비스는 Kubernetes 환경에서 운영되도록 설계되었습니다:
- ArgoCD를 통한 GitOps 기반 배포
- MySQL 영속성 볼륨 사용
- Ingress를 통한 외부 트래픽 라우팅
- Health Check 기반 무중단 배포
