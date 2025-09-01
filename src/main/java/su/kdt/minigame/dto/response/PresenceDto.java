package su.kdt.minigame.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PresenceDto {
    
    private Long groupId;
    private List<ActiveParticipant> activeParticipants;
    private int totalParticipants;
    private long timestamp;
    
    @Getter
    @Builder
    public static class ActiveParticipant {
        private String userUid;
        private String displayName;
        private String status; // JOINED, IN_PROGRESS
        private LocalDateTime lastSeenAt;
        private boolean isCurrentUser;
        
        // 진행 상황 (heartbeat에서 제공될 수 있음)
        private Integer currentRound;
        private Integer answeredCount;
        private Long elapsedMs;
    }
}