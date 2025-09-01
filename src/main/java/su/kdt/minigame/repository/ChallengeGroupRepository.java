package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import su.kdt.minigame.domain.ChallengeGroup;

import java.util.List;
import java.util.Optional;

public interface ChallengeGroupRepository extends JpaRepository<ChallengeGroup, Long> {
    
    /**
     * 활성 그룹 목록 조회
     */
    List<ChallengeGroup> findByIsActiveTrueOrderByCreatedAtDesc();
    
    /**
     * 특정 카테고리의 활성 그룹 조회
     */
    List<ChallengeGroup> findByIsActiveTrueAndCategoryOrderByCreatedAtDesc(String category);
    
    /**
     * 사용자가 생성한 그룹 조회
     */
    List<ChallengeGroup> findByCreatedByOrderByCreatedAtDesc(String createdBy);
    
    /**
     * 퀴즈 ID로 그룹 조회
     */
    List<ChallengeGroup> findByQuizIdAndIsActiveTrue(Long quizId);
    
    /**
     * 그룹 상세 정보 (참가자 수 포함)
     */
    @Query("""
        SELECT g, COUNT(e.id) as participantCount
        FROM ChallengeGroup g 
        LEFT JOIN ChallengeEntry e ON g.id = e.groupId 
        WHERE g.id = :groupId AND g.isActive = true
        GROUP BY g.id
        """)
    Optional<Object[]> findByIdWithParticipantCount(@Param("groupId") Long groupId);
}