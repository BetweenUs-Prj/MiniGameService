package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import su.kdt.minigame.domain.ReactionResult;

import java.util.List;
import java.util.Optional;

public interface ReactionResultRepo extends JpaRepository<ReactionResult, Long> {
    
    // Session-based methods for single-game reaction games
    @Query("SELECT r FROM ReactionResult r WHERE r.sessionId = :sessionId ORDER BY CASE WHEN r.falseStart = true THEN 1 ELSE 0 END, r.deltaMs ASC, r.userId ASC")
    List<ReactionResult> findBySessionIdOrderByPerformance(Long sessionId);
    
    List<ReactionResult> findBySessionIdOrderByRankOrderAsc(Long sessionId);
    
    Optional<ReactionResult> findBySessionIdAndUserId(Long sessionId, Long userId);
    
    List<ReactionResult> findBySessionId(Long sessionId);
    
    // Legacy round-based methods adapted to session-based storage
    @Query("SELECT r FROM ReactionResult r WHERE r.sessionId = (SELECT rr.sessionId FROM ReactionRound rr WHERE rr.roundId = :roundId) AND r.userId = :userUid")
    Optional<ReactionResult> findByRoundIdAndUserId(Long roundId, Long userId);
    
    @Query("SELECT r FROM ReactionResult r WHERE r.sessionId = (SELECT rr.sessionId FROM ReactionRound rr WHERE rr.roundId = :roundId) ORDER BY CASE WHEN r.falseStart = true THEN 1 ELSE 0 END, r.deltaMs ASC, r.userId ASC")
    List<ReactionResult> findByRoundIdOrderByPerformance(Long roundId);
    
    @Query("SELECT r FROM ReactionResult r WHERE r.sessionId = (SELECT rr.sessionId FROM ReactionRound rr WHERE rr.roundId = :roundId) ORDER BY r.rankOrder ASC")
    List<ReactionResult> findByRoundIdOrderByRankOrderAsc(Long roundId);
    
    @Query("SELECT r FROM ReactionResult r WHERE r.sessionId IN (SELECT rr.sessionId FROM ReactionRound rr WHERE rr.roundId IN :roundIds)")
    List<ReactionResult> findByRoundIdIn(List<Long> roundIds);
    
    // Note: Compatibility method name already matches above
}