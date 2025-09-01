package su.kdt.minigame.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 퀴즈 세션 시작 요청 DTO
 */
public record StartQuizReq(
    @NotBlank(message = "카테고리는 필수입니다")
    String category
) {
}