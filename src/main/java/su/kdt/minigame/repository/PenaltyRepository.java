package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import su.kdt.minigame.domain.Penalty;

import java.util.List;
import java.util.Optional;

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

    /**
     * 특정 사용자가 생성한 모든 벌칙을 조회합니다.
     * @param userUid 사용자의 고유 ID
     * @return 해당 사용자의 벌칙 목록
     */
    List<Penalty> findByUserUid(String userUid);

    // 기본 벌칙(소유자가 없는) 목록을 조회
    List<Penalty> findByUserUidIsNull();
    
    // 중복 방지를 위한 메소드
    boolean existsByUserUidAndText(String userUid, String text);
    
    // 특정 사용자 또는 시스템 벌칙 조회
    List<Penalty> findByUserUidOrUserUid(String userUid1, String userUid2);
    
    // slug 기반 조회 (멱등 시드용)
    Optional<Penalty> findBySlug(String slug);
}