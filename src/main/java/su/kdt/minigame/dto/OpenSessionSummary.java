package su.kdt.minigame.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class OpenSessionSummary {
    private Long sessionId;
    private String code;
    private String gameType;
    private String category;
    private String status;
    private Integer maxPlayers;
    private Long memberCount;
    private String hostUid;
    private Instant createdAt;
}