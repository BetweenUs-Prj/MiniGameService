package su.kdt.minigame.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import su.kdt.minigame.domain.QuizQuestion;

import java.util.List;

public interface QuizQuestionRepo extends JpaRepository<QuizQuestion, Long> {
  @Query("SELECT q FROM QuizQuestion q WHERE (:category IS NULL OR q.category = :category)")
  List<QuizQuestion> findByCategory(@Param("category") String category);

  @Query("""
    SELECT q FROM QuizQuestion q
     WHERE (:placeId IS NULL OR q.placeId = :placeId)
       AND (:category IS NULL OR q.category = :category)
  """)
  Page<QuizQuestion> search(@Param("placeId") Long placeId,
                            @Param("category") String category,
                            Pageable pageable);

  @Query("""
    SELECT DISTINCT TRIM(q.category)
    FROM QuizQuestion q
    WHERE q.category IS NOT NULL
      AND TRIM(q.category) <> ''
    ORDER BY TRIM(q.category) ASC
  """)
  List<String> findAllDistinctCategories();
}