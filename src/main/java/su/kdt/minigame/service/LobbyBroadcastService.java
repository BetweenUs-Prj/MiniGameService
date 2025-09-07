package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 강제 로비 동기화 서비스 - 이벤트 리스너가 작동하지 않을 경우 대비책
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LobbyBroadcastService {
    
    private final SSEService sseService;
    private final LobbyQueryService lobbyQueryService;
    
    // 마지막 브로드캐스트 버전을 세션별로 추적
    private final java.util.concurrent.ConcurrentHashMap<Long, Long> lastBroadcastVersions = 
            new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * 즉시 로비 스냅샷을 브로드캐스트합니다.
     * GameSessionService에서 직접 호출하여 실시간 동기화를 보장합니다.
     */
    public void broadcastLobbySnapshot(Long sessionId, String gameType) {
        try {
            log.info("🚨 [FORCE-BROADCAST] Starting immediate lobby broadcast - sessionId: {}, gameType: {}", 
                    sessionId, gameType);
            
            // 최신 스냅샷 조회
            LobbyQueryService.LobbySnapshot snapshot = lobbyQueryService.getLobbySnapshot(sessionId);
            
            // 버전 체크: 이미 더 최신 버전을 브로드캐스트했다면 건너뛰기
            Long lastVersion = lastBroadcastVersions.get(sessionId);
            if (lastVersion != null && snapshot.version() <= lastVersion) {
                log.info("⏭️ [FORCE-BROADCAST] Skipping stale version {} (last: {}) for sessionId: {}", 
                        snapshot.version(), lastVersion, sessionId);
                return;
            }
            
            // SSE로 브로드캐스트
            if ("quiz".equals(gameType.toLowerCase())) {
                sseService.broadcastToQuizGame(sessionId, "lobby-update", snapshot);
            } else if ("reaction".equals(gameType.toLowerCase())) {
                sseService.broadcastToReactionGame(sessionId, "lobby-update", snapshot);
            } else {
                sseService.broadcastToSession(sessionId, "lobby-update", snapshot);
            }
            
            // 브로드캐스트한 버전 기록
            lastBroadcastVersions.put(sessionId, snapshot.version());
            
            log.info("📡 [FORCE-BROADCAST] Sent lobby-update via SSE - sessionId: {}, version: {}, members: {}, count: {}", 
                    sessionId, snapshot.version(), snapshot.members().size(), snapshot.count());
            
        } catch (Exception e) {
            log.error("❌ [FORCE-BROADCAST] Failed for sessionId: " + sessionId, e);
        }
    }
    
    /**
     * 지연된 브로드캐스트 - 트랜잭션 완료 후 확실하게 전송
     */
    public void delayedBroadcast(Long sessionId, String gameType) {
        new Thread(() -> {
            try {
                Thread.sleep(200); // 트랜잭션 완료 대기
                broadcastLobbySnapshot(sessionId, gameType);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}