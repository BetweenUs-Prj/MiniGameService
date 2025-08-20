package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import su.kdt.minigame.domain.GameSession;
import su.kdt.minigame.domain.ReactionResult;

import java.util.List;

public interface ReactionResultRepo extends JpaRepository<ReactionResult, Long> {

    // ===== 아래 두 메소드를 추가해주세요! =====

    /**
     * 특정 게임 세션에 속한 모든 결과를 조회합니다.
     * @param session 조회할 게임 세션
     * @return 결과 목록
     */
    List<ReactionResult> findBySession(GameSession session);

    /**
     * 특정 게임 세션에 속한 모든 결과를 reactionTime 오름차순(빠른 순)으로 정렬하여 조회합니다.
     * @param session 조회할 게임 세션
     * @return 정렬된 결과 목록 (리더보드용)
     */
    List<ReactionResult> findBySessionOrderByReactionTimeAsc(GameSession session);
}