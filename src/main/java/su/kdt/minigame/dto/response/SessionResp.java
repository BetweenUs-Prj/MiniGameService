package su.kdt.minigame.dto.response;

import java.time.LocalDateTime;

public record SessionResp(
    Long sessionId,
    Long appointmentId,
    String gameType,
    String status,
    LocalDateTime startTime,
    LocalDateTime endTime
) {}