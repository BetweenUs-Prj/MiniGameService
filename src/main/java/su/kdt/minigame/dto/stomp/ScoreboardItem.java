package su.kdt.minigame.dto.stomp;

public record ScoreboardItem(
    String userUid,
    String nickname,
    int score
) {
}