package su.kdt.minigame.dto.stomp;

import java.util.List;

public record GameEndPayload(
    List<ScoreboardItem> finalScoreboard,
    PenaltyInfo penalty
) {
}

record PenaltyInfo(Long selectedPenaltyId, String penaltyText, String loserUid, String loserNickname) {
}