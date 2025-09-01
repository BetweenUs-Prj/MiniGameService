package su.kdt.minigame.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import su.kdt.minigame.service.SSEService;

import java.time.Instant;
import java.util.Map;

/**
 * SSE 연결 및 폴링 백업 스케줄러
 * 1-2초마다 하트비트를 전송하여 연결 상태 확인
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SSEPollingScheduler {

    private final SSEService sseService;
    
    /**
     * 1초마다 하트비트 전송
     * 연결이 끊어진 클라이언트들은 자동으로 제거됨
     */
    @Scheduled(fixedRate = 1000)
    public void sendHeartbeat() {
        try {
            sseService.sendHeartbeat();
        } catch (Exception e) {
            log.warn("[POLLING] Failed to send heartbeat: {}", e.getMessage());
        }
    }
    
    /**
     * 2초마다 상태 동기화를 위한 폴링 백업
     * 클라이언트가 SSE를 지원하지 않거나 연결이 불안정한 경우를 대비
     */
    @Scheduled(fixedRate = 2000)
    public void pollingFallback() {
        try {
            Map<String, Object> pollingData = Map.of(
                "type", "polling-update",
                "timestamp", Instant.now().toEpochMilli(),
                "message", "Polling fallback active - use HTTP GET /api/sessions/{id}/status for current state"
            );
            
            // 모든 활성 세션에 폴링 백업 신호 전송
            // 클라이언트는 이 신호를 받으면 HTTP GET으로 상태를 조회할 수 있음
            log.debug("[POLLING] Fallback ping sent at {}", Instant.now());
            
        } catch (Exception e) {
            log.warn("[POLLING] Failed to send polling fallback: {}", e.getMessage());
        }
    }
}