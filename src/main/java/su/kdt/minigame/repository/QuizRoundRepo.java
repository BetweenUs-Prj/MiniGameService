package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import su.kdt.minigame.domain.QuizRound;

import java.util.List;
import java.util.Optional;

public interface QuizRoundRepo extends JpaRepository<QuizRound, Long> {
    List<QuizRound> findBySessionId(Long sessionId);

    long countBySessionId(Long sessionId);
    
    Optional<QuizRound> findBySessionIdAndRoundNo(Long sessionId, Integer roundNo);
    
    /**
     * 세션의 라운드를 시작시간 내림차순으로 조회 (최신순)
     */
    List<QuizRound> findBySessionIdOrderByStartsAtDesc(Long sessionId);
}