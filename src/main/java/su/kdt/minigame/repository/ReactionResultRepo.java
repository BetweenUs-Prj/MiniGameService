package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import su.kdt.minigame.domain.GameSession;
import su.kdt.minigame.domain.ReactionResult;
import java.util.List;

public interface ReactionResultRepo extends JpaRepository<ReactionResult, Long> {

    // 특정 게임 세션의 모든 결과를 반응 시간이 빠른 순서대로 조회합니다.
    List<ReactionResult> findBySessionOrderByReactionTimeAsc(GameSession session);
}