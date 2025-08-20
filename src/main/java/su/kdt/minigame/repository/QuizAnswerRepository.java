package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import su.kdt.minigame.domain.QuizAnswer;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    // ===== 이 쿼리 메소드를 추가해주세요! =====
    @Query("SELECT SUM(qa.responseTimeMs) FROM QuizAnswer qa " +
           "JOIN qa.round qr " +
           "WHERE qr.session.id = :sessionId AND qa.userUid = :userUid AND qa.isCorrect = true")
    Long findTotalCorrectResponseTimeByUser(@Param("sessionId") Long sessionId, @Param("userUid") String userUid);
}