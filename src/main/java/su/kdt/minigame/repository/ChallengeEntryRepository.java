package su.kdt.minigame.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import su.kdt.minigame.domain.ChallengeEntry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChallengeEntryRepository extends JpaRepository<ChallengeEntry, Long> {
    
    /**
     * 그룹 + 사용자로 참가 엔트리 조회 (UNIQUE 제약)
     */
    Optional<ChallengeEntry> findByGroupIdAndUserUid(Long groupId, String userUid);
    
    /**
     * 그룹 리더보드 (완료된 참가자만, 점수 내림차순)
     */
    @Query("""
        SELECT e FROM ChallengeEntry e 
        WHERE e.groupId = :groupId 
        AND e.status = 'FINISHED'
        ORDER BY e.totalScore DESC, e.durationMs ASC, e.finishedAt ASC
        """)
    Page<ChallengeEntry> findLeaderboard(@Param("groupId") Long groupId, Pageable pageable);
    
    /**
     * 그룹의 전체 리더보드 (순위 업데이트용)
     */
    @Query("""
        SELECT e FROM ChallengeEntry e 
        WHERE e.groupId = :groupId 
        AND e.status = 'FINISHED'
        ORDER BY e.totalScore DESC, e.durationMs ASC, e.finishedAt ASC
        """)
    List<ChallengeEntry> findAllFinishedByGroupId(@Param("groupId") Long groupId);
    
    /**
     * Presence: 30초 이내 활동 중인 참가자들
     */
    @Query("""
        SELECT e FROM ChallengeEntry e 
        WHERE e.groupId = :groupId 
        AND e.status IN ('JOINED', 'IN_PROGRESS')
        AND e.lastSeenAt >= :cutoffTime
        ORDER BY e.lastSeenAt DESC
        """)
    List<ChallengeEntry> findActiveParticipants(
        @Param("groupId") Long groupId, 
        @Param("cutoffTime") LocalDateTime cutoffTime
    );
    
    /**
     * 그룹의 모든 참가자 수
     */
    long countByGroupId(Long groupId);
    
    /**
     * 그룹의 완료 참가자 수
     */
    long countByGroupIdAndStatus(Long groupId, ChallengeEntry.Status status);
    
    /**
     * 특정 사용자의 완료된 챌린지 목록
     */
    List<ChallengeEntry> findByUserUidAndStatusOrderByFinishedAtDesc(
        String userUid, 
        ChallengeEntry.Status status
    );
    
    /**
     * 순위 업데이트 (Batch)
     */
    @Modifying
    @Query("UPDATE ChallengeEntry e SET e.rankPosition = :rank WHERE e.id = :entryId")
    void updateRankPosition(@Param("entryId") Long entryId, @Param("rank") Integer rank);
    
    /**
     * Heartbeat: last_seen_at 업데이트
     */
    @Modifying
    @Query("UPDATE ChallengeEntry e SET e.lastSeenAt = :now WHERE e.groupId = :groupId AND e.userUid = :userUid")
    void updateLastSeen(@Param("groupId") Long groupId, @Param("userUid") String userUid, @Param("now") LocalDateTime now);
}