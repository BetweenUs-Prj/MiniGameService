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
                String.valueOf(gamePenalty.getUserId()),
                gamePenalty.getPenalty().getDescription()
        );
    }
}