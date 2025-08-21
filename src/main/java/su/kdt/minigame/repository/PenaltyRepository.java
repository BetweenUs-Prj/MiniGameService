package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import su.kdt.minigame.domain.Penalty;

import java.util.List; // import 추가

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

    // ===== 이 메소드를 추가합니다! =====
    /**
     * 특정 사용자가 생성한 모든 벌칙을 조회합니다.
     * @param userUid 사용자의 고유 ID
     * @return 해당 사용자의 벌칙 목록
     */
    List<Penalty> findByUserUid(String userUid);

    // 기본 벌칙(소유자가 없는) 목록을 조회
    List<Penalty> findByUserUidIsNull();
}