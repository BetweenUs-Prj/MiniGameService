package su.kdt.minigame.dto.request;

// userId를 userUid로 변경합니다.
public record SubmitAnswerReq(
        String userUid,
        String answerText
) {
}