package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import su.kdt.minigame.domain.ReactionResult;

import java.util.List;
import java.util.Optional;

public interface ReactionResultRepo extends JpaRepository<ReactionResult, Long> {
    
    // Session-based methods removed as entity now uses round-based structure
    
    /**
     * 라운드별 성능순 정렬
     */
    @Query("SELECT r FROM ReactionResult r WHERE r.roundId = :roundId ORDER BY CASE WHEN r.falseStart = true THEN 1 ELSE 0 END, r.deltaMs ASC, r.userUid ASC")
    List<ReactionResult> findByRoundIdOrderByPerformance(Long roundId);
    
    /**
     * 라운드와 사용자별 결과 조회
     */
    Optional<ReactionResult> findByRoundIdAndUserUid(Long roundId, String userUid);
    
    /**
     * 라운드별 참가자 존재 여부 확인
     */
    boolean existsByRoundIdAndUserUid(Long roundId, String userUid);
    
    /**
     * 여러 라운드의 결과를 한번에 조회
     */
    List<ReactionResult> findByRoundIdIn(List<Long> roundIds);
    
    /**
     * 라운드별 순위순 정렬
     */
    List<ReactionResult> findByRoundIdOrderByRankOrderAsc(Long roundId);
    
    // Note: Compatibility method name already matches above
}