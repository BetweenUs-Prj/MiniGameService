package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import su.kdt.minigame.domain.GameSession;
import su.kdt.minigame.domain.QuizRound;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface QuizRoundRepo extends JpaRepository<QuizRound, Long> {
    List<QuizRound> findBySession(GameSession session);

    // ✅ 자동 종료 로직에서 사용
    long countBySession(GameSession session);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM QuizRound r WHERE r.id = :roundId")
    Optional<QuizRound> findByIdWithLock(@Param("roundId") Long roundId);

    /**
     * 특정 게임 세션의 가장 마지막 라운드를 찾습니다.
     */
    Optional<QuizRound> findTopBySessionOrderByIdDesc(GameSession session);
}