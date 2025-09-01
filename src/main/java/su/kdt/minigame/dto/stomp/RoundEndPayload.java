package su.kdt.minigame.dto.stomp;

import java.time.LocalDateTime;
import java.util.List;

public record RoundEndPayload(
    int roundNo,
    List<ScoreboardItem> scoreboard,
    LocalDateTime nextRoundAt,
    String reason // 'ALL_ANSWERED' | 'TIMEOUT'
) {
}