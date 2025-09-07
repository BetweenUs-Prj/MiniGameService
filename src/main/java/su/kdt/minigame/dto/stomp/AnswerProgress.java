package su.kdt.minigame.dto.stomp;

public record AnswerProgress(
    int roundNo,
    long answeredCount,
    long totalPlayers
) {
}