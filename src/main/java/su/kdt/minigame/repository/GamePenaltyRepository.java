package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import su.kdt.minigame.domain.GamePenalty;
import su.kdt.minigame.domain.GamePenaltyId;

import java.util.Optional;

public interface GamePenaltyRepository extends JpaRepository<GamePenalty, GamePenaltyId> {

    /**
     * 게임 세션 ID를 기준으로 할당된 벌칙을 조회합니다.
     * @param sessionId 게임 세션의 ID
     * @return Optional<GamePenalty>
     */
    Optional<GamePenalty> findByGameSessionId(Long sessionId);
}