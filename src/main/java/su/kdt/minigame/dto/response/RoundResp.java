package su.kdt.minigame.dto.response;

import java.time.LocalDateTime;

public record RoundResp(
    Long roundId,
    Long sessionId,
    LocalDateTime startTime
) {}