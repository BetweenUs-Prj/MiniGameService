package su.kdt.minigame.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.service.GameLifecycleService;
import su.kdt.minigame.service.LobbyQueryService;
import su.kdt.minigame.service.SSEService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 폴링 백업 컨트롤러
 * SSE가 지원되지 않거나 연결이 불안정한 환경에서 HTTP 폴링으로 대체
 */
@Slf4j
@RestController
@RequestMapping("/api/polling")
@RequiredArgsConstructor
public class PollingController {

    private final GameLifecycleService gameLifecycleService;
    private final LobbyQueryService lobbyQueryService;
    private final SSEService sseService;

    /**
     * 세션 상태 폴링 엔드포인트
     * 클라이언트가 1-2초마다 호출하여 최신 상태 확인
     */
    @GetMapping("/sessions/{sessionId}/state")
    public ResponseEntity<Map<String, Object>> getSessionState(@PathVariable Long sessionId) {
        try {
            Map<String, Object> sessionState = gameLifecycleService.syncGameState(sessionId, "polling-client");
            Map<String, Object> sseStatus = sseService.getSessionStatus(sessionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("session", sessionState);
            response.put("sse", sseStatus);
            response.put("timestamp", Instant.now().toEpochMilli());
            response.put("polling", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.warn("[POLLING] Failed to get session state for {}: {}", sessionId, e.getMessage());
            return ResponseEntity.ok(Map.of(
                "error", e.getMessage(),
                "timestamp", Instant.now().toEpochMilli(),
                "polling", true
            ));
        }
    }

    /**
     * 로비 상태 폴링 엔드포인트
     */
    @GetMapping("/sessions/{sessionId}/lobby")
    public ResponseEntity<Map<String, Object>> getLobbyState(@PathVariable Long sessionId) {
        try {
            LobbyQueryService.LobbySnapshot snapshot = lobbyQueryService.getLobbySnapshot(sessionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("lobby", snapshot);
            response.put("timestamp", Instant.now().toEpochMilli());
            response.put("polling", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.warn("[POLLING] Failed to get lobby state for {}: {}", sessionId, e.getMessage());
            return ResponseEntity.ok(Map.of(
                "error", e.getMessage(),
                "timestamp", Instant.now().toEpochMilli(),
                "polling", true
            ));
        }
    }

    /**
     * 하트비트 엔드포인트 - 연결 상태 확인
     */
    @GetMapping("/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat() {
        return ResponseEntity.ok(Map.of(
            "status", "alive",
            "timestamp", Instant.now().toEpochMilli(),
            "message", "Polling service active"
        ));
    }
}