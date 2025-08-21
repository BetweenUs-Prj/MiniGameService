package su.kdt.minigame.dto.request;

public record CreateSessionReq(
        Long appointmentId,  // 약속 ID
        String gameType,     // "QUIZ" | "REACTION"
        Long penaltyId,      // 선택 벌칙 ID
        Integer totalRounds  // 라운드 수(퀴즈용, null이면 기본값 사용)
) {}
