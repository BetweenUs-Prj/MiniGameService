package su.kdt.minigame.dto.response;

import su.kdt.minigame.domain.GameSession;
import java.time.LocalDateTime;

public record SessionResp(
        Long sessionId,
        Long appointmentId,
        String gameType,
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
    // ===== 아래 메소드를 새로 추가해주세요! =====
    public static SessionResp from(GameSession session) {
        return new SessionResp(
                session.getId(),
                session.getAppointmentId(),
                session.getGameType().name(), // Enum을 String으로 변환
                session.getStatus().name(),   // Enum을 String으로 변환
                session.getStartTime(),
                session.getEndTime()
        );
    }
    // ===== 여기까지 추가 =====
}