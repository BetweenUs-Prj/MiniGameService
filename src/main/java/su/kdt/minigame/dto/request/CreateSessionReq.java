package su.kdt.minigame.dto.request;

public record CreateSessionReq(
        Long appointmentId,
        String gameType,
        Long penaltyId // ◀◀◀ 선택한 벌칙의 ID를 받을 필드 추가
) {
}