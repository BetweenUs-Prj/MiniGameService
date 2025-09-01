package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import su.kdt.minigame.domain.GameSession;
import su.kdt.minigame.domain.GameSessionMember;
import su.kdt.minigame.repository.GameRepo;
import su.kdt.minigame.repository.GameSessionMemberRepo;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Single source of truth for lobby snapshots.
 * Ensures host is always included in members array and count === members.size().
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LobbyQueryService {
    
    private final GameRepo gameRepo;
    private final GameSessionMemberRepo memberRepo;
    
    // 단조 증가 버전을 위한 AtomicLong
    private static final java.util.concurrent.atomic.AtomicLong VERSION_COUNTER = 
            new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());
    
    /**
     * Creates a unified lobby snapshot with guaranteed consistency:
     * - Host is always included in members array with role "HOST"
     * - Other participants have role "MEMBER"
     * - count always equals members.size()
     */
    public LobbySnapshot getLobbySnapshot(Long sessionId) {
        GameSession session = gameRepo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        
        // Get all session members from database
        List<GameSessionMember> sessionMembers = memberRepo.findBySessionIdOrderByJoinedAt(sessionId);
        
        // Create member list ensuring host is always included
        Map<String, MemberInfo> memberMap = new LinkedHashMap<>();
        
        // First, add host with role "HOST" (always present)
        memberMap.put(session.getHostUid(), new MemberInfo(
                session.getHostUid(),
                "HOST", // Host role
                0, // Host score - always 0 in lobby
                LocalDateTime.now() // Use current time as placeholder
        ));
        
        // Then, add all other session members with role "MEMBER"
        for (GameSessionMember member : sessionMembers) {
            if (!member.getUserUid().equals(session.getHostUid())) {
                memberMap.put(member.getUserUid(), new MemberInfo(
                        member.getUserUid(),
                        "MEMBER", // Guest role
                        0, // Guest score - always 0 in lobby
                        member.getJoinedAt()
                ));
            } else {
                // If host is also in GameSessionMember table, update with actual joined time
                memberMap.put(member.getUserUid(), new MemberInfo(
                        member.getUserUid(),
                        "HOST",
                        0,
                        member.getJoinedAt()
                ));
            }
        }
        
        List<MemberInfo> members = new ArrayList<>(memberMap.values());
        int count = members.size();
        
        // Enhanced defensive logging to ensure consistency
        log.info("[LOBBY-SNAPSHOT] Session {}: members.size()={}, count={}, host={}, members=[{}]",
                sessionId, members.size(), count, session.getHostUid(),
                members.stream().map(m -> m.uid() + ":" + m.role()).collect(Collectors.joining(",")));
        
        // Verify consistency - this should never happen with our logic
        if (count != members.size()) {
            log.error("[LOBBY] CONSISTENCY ERROR: count {} != members.size() {} for session {}", 
                    count, members.size(), sessionId);
            throw new IllegalStateException("Lobby snapshot consistency error: count != members.size()");
        }
        
        // Generate new version for this snapshot
        long version = VERSION_COUNTER.incrementAndGet();
        
        return new LobbySnapshot(
                "LOBBY_SNAPSHOT", // Unified event type
                sessionId,
                members,
                count,
                version,
                System.currentTimeMillis()
        );
    }
    
    /**
     * Unified lobby snapshot format matching frontend expectations
     */
    public record LobbySnapshot(
            String type,
            Long sessionId,
            List<MemberInfo> members,
            int count,
            long version,
            long timestamp
    ) {
        // Helper methods for compatibility with legacy code
        public long total() { return count; }
        public int capacity() { return 10; } // Max players from SessionConfig, hardcoded for now
    }
    
    /**
     * Member info with role and score for lobby display
     */
    public record MemberInfo(
            String uid,
            String role, // "HOST" or "MEMBER"
            int score,
            LocalDateTime joinedAt
    ) {
        // Helper methods for compatibility with legacy code
        public String userUid() { return uid; }
        public boolean isReady() { 
            // For now, all members are considered ready in lobby
            // This could be extended to read from GameSessionMember.isReady if needed
            return true; 
        }
    }
}