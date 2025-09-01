package su.kdt.minigame.dto.stomp;

import su.kdt.minigame.dto.response.QuizQuestionResp;
import java.time.LocalDateTime;
import java.util.List;

public record RoundStartPayload(
    int roundNo,
    QuizQuestionResp question,
    LocalDateTime expiresAt,
    List<ScoreboardItem> scoreboard
) {
}