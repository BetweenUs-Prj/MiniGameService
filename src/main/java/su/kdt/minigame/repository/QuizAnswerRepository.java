package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import su.kdt.minigame.domain.QuizAnswer;
import su.kdt.minigame.domain.QuizRound;
import java.util.Optional;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    // 특정 사용자의 '정답 응답 시간' 총합을 계산 (기존 메소드)
    @Query("SELECT SUM(qa.responseTimeMs) FROM QuizAnswer qa " +
           "JOIN qa.round qr " +
           "WHERE qr.sessionId = :sessionId AND qa.userUid = :userUid AND qa.isCorrect = true")
    Long findTotalCorrectResponseTimeByUser(@Param("sessionId") Long sessionId, @Param("userUid") String userUid);

    // ===== 아래 메소드를 추가해주세요! =====
    /**
     * 특정 게임 세션에서 사용자가 맞힌 정답의 총 개수를 계산합니다.
     */
    @Query("SELECT COUNT(qa) FROM QuizAnswer qa " +
           "JOIN qa.round qr " +
           "WHERE qr.sessionId = :sessionId AND qa.userUid = :userUid AND qa.isCorrect = true")
    Long countCorrectAnswersByUser(@Param("sessionId") Long sessionId, @Param("userUid") String userUid);

    /**
     * 특정 라운드에 몇 명의 사용자가 답변을 제출했는지 셉니다.
     */
    @Query("SELECT COUNT(DISTINCT qa.userUid) FROM QuizAnswer qa WHERE qa.round = :round")
    long countDistinctUserUidsByRound(@Param("round") QuizRound round);
    
    /**
     * 특정 라운드에 답변을 제출한 사용자 수를 계산합니다 (round ID 기준).
     */
    long countByRoundRoundId(Long roundId);
    
    /**
     * 특정 라운드에 특정 사용자가 이미 답변을 제출했는지 확인합니다.
     */
    boolean existsByRoundRoundIdAndUserUid(Long roundId, String userUid);
    
    /**
     * 특정 라운드와 사용자로 이미 답변이 존재하는지 확인합니다.
     */
    boolean existsByRoundAndUserUid(QuizRound round, String userUid);
    
    /**
     * 특정 세션의 사용자별 총 점수를 조회합니다.
     */
    @Query("SELECT SUM(qa.score) FROM QuizAnswer qa " +
           "JOIN qa.round qr " +
           "WHERE qr.sessionId = :sessionId AND qa.userUid = :userUid")
    Integer sumScoreBySessionIdAndUserUid(@Param("sessionId") Long sessionId, @Param("userUid") String userUid);

    /**
     * 특정 세션에서 사용자가 제출한 총 답변 수를 조회합니다.
     */
    @Query("SELECT COUNT(qa) FROM QuizAnswer qa " +
           "JOIN qa.round qr " +
           "WHERE qr.sessionId = :sessionId AND qa.userUid = :userUid")
    Long countAnswersByUser(@Param("sessionId") Long sessionId, @Param("userUid") String userUid);
    
    /**
     * 특정 라운드와 사용자의 답변을 조회합니다.
     */
    Optional<QuizAnswer> findByRoundAndUserUid(QuizRound round, String userUid);
}
