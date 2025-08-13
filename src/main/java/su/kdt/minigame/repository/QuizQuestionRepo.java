package su.kdt.minigame.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import su.kdt.minigame.domain.QuizQuestion;

public interface QuizQuestionRepo extends JpaRepository<QuizQuestion, Long> {
  @Query("""
    SELECT q FROM QuizQuestion q
     WHERE (:placeId IS NULL OR q.placeId = :placeId)
       AND (:category IS NULL OR q.category = :category)
  """)
  Page<QuizQuestion> search(@Param("placeId") Long placeId,
                            @Param("category") String category,
                            Pageable pageable);
}