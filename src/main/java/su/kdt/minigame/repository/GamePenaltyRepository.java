package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import su.kdt.minigame.domain.GamePenalty;
import su.kdt.minigame.domain.GamePenaltyId;

public interface GamePenaltyRepository extends JpaRepository<GamePenalty, GamePenaltyId> {
}