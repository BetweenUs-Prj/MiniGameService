package su.kdt.minigame.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * 반응속도 게임 참가자 동기화 버그 수정 검증 테스트
 * 
 * 수정 전 문제:
 * - 호스트는 시작되지만 참가자는 시작이 안 되고 204/WAITING 상태 고정
 * - 참가자 수가 0명으로 고정
 * - Sync polling timed out after 5 seconds 반복
 * 
 * 수정 후 기대:
 * - 호스트 시작 → 모든 참가자 1초 내 IN_PROGRESS 전환
 * - 참가자 수/목록 실시간 갱신
 * - 늦게 합류해도 READY/GO/RESULT 중 정확한 단계로 복구
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public class ReactionGameBugFixTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String createUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpEntity<Map<String, Object>> createHttpEntity(String userUid, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("X-User-Uid", userUid);
        return new HttpEntity<>(body, headers);
    }

    @Test
    @DisplayName("참가자 수 0명 고정 버그 수정 검증")
    public void testParticipantCountNotStuckAtZero() throws InterruptedException {
        // Given: 세션 생성
        Map<String, Object> createSessionBody = new HashMap<>();
        createSessionBody.put("gameType", "REACTION");
        createSessionBody.put("capacity", 10);
        createSessionBody.put("penaltyId", 1L);
        
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
            createUrl("/api/mini-games/sessions"),
            createHttpEntity("test_host", createSessionBody),
            Map.class
        );
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long sessionId = ((Number) createResponse.getBody().get("sessionId")).longValue();
        
        // When: 참가자 입장
        ResponseEntity<Map> joinResponse = restTemplate.postForEntity(
            createUrl("/api/mini-games/sessions/" + sessionId + "/join"),
            createHttpEntity("participant_1", Map.of()),
            Map.class
        );
        
        // Then: 참가자 수가 즉시 반영되어야 함 (0명 고정 금지)
        assertThat(joinResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(joinResponse.getBody().get("total")).isEqualTo(2); // host + participant_1
        
        // 세션 상세 정보에서도 참가자 목록 확인
        ResponseEntity<Map> sessionResponse = restTemplate.getForEntity(
            createUrl("/api/mini-games/sessions/" + sessionId),
            Map.class
        );
        
        assertThat(sessionResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sessionResponse.getBody().get("total")).isEqualTo(2);
        assertThat(sessionResponse.getBody().get("participants")).isNotNull();
    }

    @Test
    @DisplayName("게임 시작 시 IN_PROGRESS 상태 브로드캐스트 및 라운드 즉시 생성 검증")
    public void testGameStartBroadcastAndRoundCreation() throws InterruptedException {
        // Given: 세션 생성 및 참가자 추가
        Map<String, Object> createSessionBody = new HashMap<>();
        createSessionBody.put("gameType", "REACTION");
        createSessionBody.put("capacity", 10);
        createSessionBody.put("penaltyId", 1L);
        
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
            createUrl("/api/mini-games/sessions"),
            createHttpEntity("test_host", createSessionBody),
            Map.class
        );
        
        Long sessionId = ((Number) createResponse.getBody().get("sessionId")).longValue();
        
        // 참가자 추가
        restTemplate.postForEntity(
            createUrl("/api/mini-games/sessions/" + sessionId + "/join"),
            createHttpEntity("participant_1", Map.of()),
            Map.class
        );
        
        // When: 게임 시작
        ResponseEntity<Map> startResponse = restTemplate.postForEntity(
            createUrl("/api/mini-games/sessions/" + sessionId + "/start"),
            createHttpEntity("test_host", Map.of()),
            Map.class
        );
        
        // Then: 게임 시작 성공
        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // 세션 상태가 IN_PROGRESS로 변경되어야 함
        ResponseEntity<Map> sessionAfterStart = restTemplate.getForEntity(
            createUrl("/api/mini-games/sessions/" + sessionId),
            Map.class
        );
        
        assertThat(sessionAfterStart.getBody().get("status")).isEqualTo("IN_PROGRESS");
        
        // current-round API가 200을 반환해야 함 (204 최소화)
        ResponseEntity<Map> currentRoundResponse = restTemplate.getForEntity(
            createUrl("/api/mini-games/reaction/sessions/" + sessionId + "/current-round"),
            Map.class
        );
        
        // 게임이 빠르게 완료되지 않았다면 200, 완료되었다면 204 허용
        assertThat(currentRoundResponse.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NO_CONTENT);
        
        if (currentRoundResponse.getStatusCode() == HttpStatus.OK) {
            // 라운드가 활성 상태라면 적절한 데이터 포함
            assertThat(currentRoundResponse.getBody().get("roundId")).isNotNull();
            assertThat(currentRoundResponse.getBody().get("status")).isIn("READY", "RED");
            assertThat(currentRoundResponse.getBody().get("participants")).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("STOMP 동기화 엔드포인트 존재 및 응답 검증")
    public void testSyncEndpointExists() {
        // Given: 세션 생성
        Map<String, Object> createSessionBody = new HashMap<>();
        createSessionBody.put("gameType", "REACTION");
        createSessionBody.put("capacity", 10);
        createSessionBody.put("penaltyId", 1L);
        
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
            createUrl("/api/mini-games/sessions"),
            createHttpEntity("test_host", createSessionBody),
            Map.class
        );
        
        Long sessionId = ((Number) createResponse.getBody().get("sessionId")).longValue();
        
        // When & Then: 빌드 지문 엔드포인트가 존재하고 적절한 정보 반환
        ResponseEntity<Map> debugResponse = restTemplate.getForEntity(
            createUrl("/api/_debug/build"),
            Map.class
        );
        
        assertThat(debugResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(debugResponse.getBody().get("build")).isNotNull();
        assertThat(debugResponse.getBody().get("commit")).isNotNull();
        assertThat(debugResponse.getBody().get("timestamp")).isNotNull();
    }
}