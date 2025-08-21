package su.kdt.minigame.dto.response;

import su.kdt.minigame.domain.GamePenalty;

public record GamePenaltyResp(
        Long sessionId,
        String loserUid,
        String penaltyDescription
) {
    public static GamePenaltyResp from(GamePenalty gamePenalty) {
        return new GamePenaltyResp(
                gamePenalty.getGameSession().getId(),
                gamePenalty.getUserUid(),
                gamePenalty.getPenalty().getDescription()
        );
    }
}