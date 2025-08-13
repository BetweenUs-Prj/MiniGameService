package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import su.kdt.minigame.domain.QuizAnswer;

public interface QuizAnswerRepo extends JpaRepository<QuizAnswer, Long> {
}