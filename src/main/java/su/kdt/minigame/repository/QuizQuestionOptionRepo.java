package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import su.kdt.minigame.domain.QuizQuestion;
import su.kdt.minigame.domain.QuizQuestionOption;
import java.util.List;

public interface QuizQuestionOptionRepo extends JpaRepository<QuizQuestionOption, Long> {
    List<QuizQuestionOption> findByQuestion(QuizQuestion question);
    
    /**
     * 특정 라운드의 질문에 특정 옵션이 속하는지 확인합니다.
     */
    @Query("SELECT COUNT(qo) > 0 FROM QuizQuestionOption qo " +
           "JOIN qo.question q " +
           "JOIN QuizRound r ON r.question.id = q.id " +
           "WHERE r.roundId = :roundId AND qo.optionId = :optionId")
    boolean existsByRoundIdAndOptionId(@Param("roundId") Long roundId, @Param("optionId") Long optionId);
}