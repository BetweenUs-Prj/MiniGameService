package su.kdt.minigame.dto.response;

import su.kdt.minigame.domain.QuizRound;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record RoundResp(
        Long roundId,
        Long sessionId,
        Integer roundNo,
        QuizQuestionResp question,
        LocalDateTime expiresAt,
        Long expireAtMillis
) {
    public static RoundResp from(QuizRound round) {
        long expireAtMillis = round.getExpiresAt() != null ? 
                round.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 
                System.currentTimeMillis() + 30_000L;
                
        return new RoundResp(
                round.getRoundId(),
                round.getSessionId(),
                round.getRoundNo(),
                QuizQuestionResp.from(round.getQuestion()),
                round.getExpiresAt(),
                expireAtMillis
        );
    }
    
    // Legacy method for backward compatibility
    public static RoundResp from(QuizRound round, QuizQuestionResp question) {
        long expireAtMillis = round.getExpiresAt() != null ? 
                round.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 
                System.currentTimeMillis() + 30_000L;
                
        return new RoundResp(
                round.getRoundId(),
                round.getSessionId(),
                round.getRoundNo(),
                question,
                round.getExpiresAt(),
                expireAtMillis
        );
    }
}