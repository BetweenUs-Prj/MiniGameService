package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import su.kdt.minigame.domain.QuizQuestionOption;

public interface QuizQuestionOptionRepository extends JpaRepository<QuizQuestionOption, Long> {
    
    /**
     * 특정 라운드의 질문에 특정 옵션이 속하는지 확인합니다.
     */
    @Query("SELECT COUNT(qo) > 0 FROM QuizQuestionOption qo " +
           "JOIN qo.question q " +
           "JOIN QuizRound r ON r.question.id = q.id " +
           "WHERE r.roundId = :roundId AND qo.optionId = :optionId")
    boolean existsByRoundIdAndOptionId(@Param("roundId") Long roundId, @Param("optionId") Long optionId);
}