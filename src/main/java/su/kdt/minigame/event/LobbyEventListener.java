package su.kdt.minigame.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import su.kdt.minigame.service.LobbyQueryService;

/**
 * 트랜잭션 커밋 후 STOMP 브로드캐스트를 처리하는 이벤트 리스너
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LobbyEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final LobbyQueryService lobbyQueryService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberJoined(LobbyEvents.MemberJoinedEvent event) {
        try {
            log.info("🟢 [LOBBY-EVENT] MEMBER_JOINED - sessionId: {}, userUid: {}, gameType: {}", 
                    event.getSessionId(), event.getUserUid(), event.getGameType());

            // Get unified snapshot from single source of truth
            LobbyQueryService.LobbySnapshot snapshot = lobbyQueryService.getLobbySnapshot(event.getSessionId());
            
            // Broadcast full snapshot (not incremental event) to ensure consistency
            String topic = "/topic/" + event.getGameType().toLowerCase() + "/" + event.getSessionId() + "/lobby";
            messagingTemplate.convertAndSend(topic, snapshot);
            
            log.info("📡 [LOBBY-EVENT] BROADCAST SUCCESS to {} - members: {}, count: {}", 
                    topic, snapshot.members().size(), snapshot.count());
            
        } catch (Exception e) {
            log.error("❌ [LOBBY-EVENT] BROADCAST FAILED for member joined event", e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberLeft(LobbyEvents.MemberLeftEvent event) {
        try {
            log.info("[LOBBY-EVENT] Processing member left after transaction commit - sessionId: {}, userUid: {}", 
                    event.getSessionId(), event.getUserUid());

            // Get unified snapshot from single source of truth (member already removed)
            LobbyQueryService.LobbySnapshot snapshot = lobbyQueryService.getLobbySnapshot(event.getSessionId());
            
            // Broadcast full snapshot (not incremental event) to ensure consistency
            String topic = "/topic/" + event.getGameType().toLowerCase() + "/" + event.getSessionId() + "/lobby";
            messagingTemplate.convertAndSend(topic, snapshot);
            
            log.info("[LOBBY-EVENT] LOBBY_SNAPSHOT broadcasted to {} - members.size(): {}, count: {}", 
                    topic, snapshot.members().size(), snapshot.count());
            
        } catch (Exception e) {
            log.error("[LOBBY-EVENT] Failed to broadcast member left event", e);
        }
    }
}