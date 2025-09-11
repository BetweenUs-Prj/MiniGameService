package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import su.kdt.minigame.domain.ReactionResult;

import java.util.List;
import java.util.Optional;

public interface ReactionResultRepo extends JpaRepository<ReactionResult, Long> {
    
    // Round-based methods only - no more session-based methods
    @Query("SELECT r FROM ReactionResult r WHERE r.roundId = :roundId AND r.userId = :userId")
    Optional<ReactionResult> findByRoundIdAndUserId(Long roundId, Long userId);
    
    @Query("SELECT r FROM ReactionResult r WHERE r.roundId = :roundId ORDER BY CASE WHEN r.falseStart = true THEN 1 ELSE 0 END, r.deltaMs ASC, r.userId ASC")
    List<ReactionResult> findByRoundIdOrderByPerformance(Long roundId);
    
    @Query("SELECT r FROM ReactionResult r WHERE r.roundId = :roundId ORDER BY r.rankOrder ASC")
    List<ReactionResult> findByRoundIdOrderByRankOrderAsc(Long roundId);
    
    @Query("SELECT r FROM ReactionResult r WHERE r.roundId IN :roundIds")
    List<ReactionResult> findByRoundIdIn(List<Long> roundIds);
}