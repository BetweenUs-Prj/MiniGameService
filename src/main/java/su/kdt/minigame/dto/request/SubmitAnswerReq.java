package su.kdt.minigame.dto.request;

import java.time.LocalDateTime;

public record SubmitAnswerReq(Long userId, String answerText, LocalDateTime answerTime) {}