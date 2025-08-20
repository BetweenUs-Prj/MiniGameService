package su.kdt.minigame.dto.response;

import su.kdt.minigame.domain.QuizRound;
import java.time.LocalDateTime;

public record RoundResp(
        Long roundId,
        Long sessionId,
        LocalDateTime startTime
) {
    // ===== 이 메소드를 추가해주세요! =====
    public static RoundResp from(QuizRound round) {
        return new RoundResp(
                round.getId(),
                round.getSession().getId(),
                round.getStartTime()
        );
    }
}