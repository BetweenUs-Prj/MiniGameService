package su.kdt.minigame.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게임 결과 응답 - 승자/순위/벌칙 정보를 포함하는 단일 계약
 * WS 브로드캐스트와 REST API에서 동일하게 사용
 */
@Builder
public record GameResultsResp(
    Long sessionId,
    String gameType,
    PlayerResult winner,
    List<PlayerResult> ranking,
    PenaltyResult penalty,
    LocalDateTime completedAt
) {
    
    @Builder
    public record PlayerResult(
        String uid,
        String name,
        int score,
        int rank
    ) {}
    
    @Builder
    public record PenaltyResult(
        boolean assigned,
        String rule,
        List<PlayerResult> targets,
        String content
    ) {}
}