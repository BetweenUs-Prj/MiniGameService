package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import su.kdt.minigame.domain.QuizQuestion;
import su.kdt.minigame.domain.QuizQuestionOption;
import java.util.List;

public interface QuizQuestionOptionRepo extends JpaRepository<QuizQuestionOption, Long> {
    List<QuizQuestionOption> findByQuestion(QuizQuestion question);
    
    @Query("SELECT o FROM QuizQuestionOption o WHERE o.question.id = :questionId")
    List<QuizQuestionOption> findByQuestionId(@Param("questionId") Long questionId);
}