package su.kdt.minigame.dto.response;

public record AnswerResp(
    Long answerId,
    boolean isCorrect,
    Long winnerUserId,
    boolean roundClosed
) {}