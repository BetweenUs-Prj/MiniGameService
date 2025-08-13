package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import su.kdt.minigame.domain.QuizQuestion;
import su.kdt.minigame.domain.QuizQuestionOption;
import java.util.List;

public interface QuizQuestionOptionRepo extends JpaRepository<QuizQuestionOption, Long> {
    List<QuizQuestionOption> findByQuestion(QuizQuestion question);
}