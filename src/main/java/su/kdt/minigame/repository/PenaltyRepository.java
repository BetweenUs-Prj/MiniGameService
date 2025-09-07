package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import su.kdt.minigame.domain.Penalty;

import java.util.List;
import java.util.Optional;

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

    /**
     * 특정 사용자가 생성한 모든 벌칙을 조회합니다.
     * @param userId 사용자의 ID
     * @return 해당 사용자의 벌칙 목록
     */
    List<Penalty> findByUserId(Long userId);

    // 기본 벌칙(소유자가 없는) 목록을 조회
    List<Penalty> findByUserIdIsNull();
    
    // 중복 방지를 위한 메소드
    boolean existsByUserIdAndText(Long userId, String text);
    
    // 특정 사용자 또는 시스템 벌칙 조회
    List<Penalty> findByUserIdOrUserId(Long userId1, Long userId2);
    
    // slug 기반 조회 (멱등 시드용)
    Optional<Penalty> findBySlug(String slug);
    
    // Backward compatibility methods (String-based user IDs)
    default List<Penalty> findByUserUid(String userUid) {
        Long userId = "system".equals(userUid) ? 0L : Long.valueOf(userUid);
        return findByUserId(userId);
    }
    
    default List<Penalty> findByUserUidOrUserUid(String userUid1, String userUid2) {
        Long userId1 = "system".equals(userUid1) ? 0L : Long.valueOf(userUid1);
        Long userId2 = "system".equals(userUid2) ? 0L : Long.valueOf(userUid2);
        return findByUserIdOrUserId(userId1, userId2);
    }
}