package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import su.kdt.minigame.domain.ReactionRound;

import java.util.List;
import java.util.Optional;

public interface ReactionRoundRepo extends JpaRepository<ReactionRound, Long> {
    
    /**
     * 세션의 가장 최근 라운드 조회
     */
    Optional<ReactionRound> findTopBySessionIdOrderByRoundIdDesc(Long sessionId);
    
    /**
     * 세션의 특정 상태 라운드 조회
     */
    Optional<ReactionRound> findBySessionIdAndStatus(Long sessionId, String status);
    
    /**
     * 세션의 모든 라운드 조회
     */
    List<ReactionRound> findBySessionId(Long sessionId);
}