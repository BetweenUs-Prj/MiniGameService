package su.kdt.minigame.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import su.kdt.minigame.domain.GameSession;
import su.kdt.minigame.dto.OpenSessionSummary;
import java.util.List;
import java.util.Optional;

public interface GameRepo extends JpaRepository<GameSession, Long> {
    List<GameSession> findByAppointmentId(Long appointmentId);
    
    @Query(value = """
        SELECT 
          s.id as sessionId,
          CAST(s.id as CHAR) as code,
          s.game_type as gameType,
          s.category as category,
          s.status as status,
          10 as maxPlayers,
          COALESCE(COUNT(m.session_id), 0) as memberCount,
          s.host_uid as hostUid,
          s.created_at as createdAt
        FROM game_session s
        LEFT JOIN game_session_member m ON m.session_id = s.id
        WHERE (:gameType is null or s.game_type = :gameType)
          AND (:status is null or s.status = :status)  
          AND s.status = 'WAITING'
          AND (:q is null or s.host_uid like concat('%', :q, '%'))
        GROUP BY s.id, s.game_type, s.category, s.status, s.host_uid, s.created_at
        ORDER BY s.id desc
        """, nativeQuery = true)
    Page<OpenSessionSummary> findOpenSessions(
        @Param("gameType") String gameType,
        @Param("status") String status,
        @Param("q") String q,
        Pageable pageable
    );

    // 초대 코드 관련 메서드들 - 메모리나 Redis로 관리
    default Optional<GameSession> findByCode(String code) {
        // 코드가 FRIEND- 접두사로 시작하는지 확인하고 처리
        if (code != null && code.startsWith("FRIEND-")) {
            String sessionIdStr = code.substring(7); // "FRIEND-" 제거
            try {
                Long sessionId = Long.parseLong(sessionIdStr);
                return findById(sessionId);
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}