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
    
    @Query("""
        select new su.kdt.minigame.dto.OpenSessionSummary(
          s.id,
          CAST(s.id as string),
          CAST(s.gameType as string),
          s.category,
          CAST(s.status as string),
          10,
          COUNT(m.sessionId),
          s.hostUid,
          null
        )
        from GameSession s
        left join GameSessionMember m on m.sessionId = s.id
        where (:gameType is null or CAST(s.gameType as string) = :gameType)
          and (:status is null or CAST(s.status as string) = :status)
          and s.status = 'WAITING'
          and (:q is null or s.hostUid like %:q%)
        group by s.id, s.gameType, s.status, s.hostUid
        order by s.id desc
        """)
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