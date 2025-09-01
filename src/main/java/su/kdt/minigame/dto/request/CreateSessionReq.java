package su.kdt.minigame.dto.request;

public record CreateSessionReq(
        Long appointmentId,  // 약속 ID
        String gameType,     // "QUIZ" | "REACTION"
        Long penaltyId,      // 선택 벌칙 ID
        Integer totalRounds, // 라운드 수(퀴즈용, null이면 기본값 사용)
        String category,     // 카테고리 ("일반", "술", etc.)
        Boolean inviteOnly,  // 친구만 허용(초대 전용) 옵션 - deprecated, use isPrivate
        Boolean isPrivate,   // 비공개방 여부
        String pin          // PIN 코드 (4자리, isPrivate=true일 때만 필수)
) {}
