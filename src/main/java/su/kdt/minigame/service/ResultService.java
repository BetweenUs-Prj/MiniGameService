package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su.kdt.minigame.domain.GameSession;
import su.kdt.minigame.domain.Penalty;
import su.kdt.minigame.dto.response.GameResultDto;
import su.kdt.minigame.dto.response.PlayerScore;
import su.kdt.minigame.dto.response.ScoreboardDto;
import su.kdt.minigame.exception.NotFoundException;
import su.kdt.minigame.exception.UnprocessableEntityException;
import su.kdt.minigame.repository.GameRepo;
import su.kdt.minigame.repository.QuizAnswerRepository;
import su.kdt.minigame.repository.GamePenaltyRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 게임 결과 서비스 (REST 전용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService {
    
    private final GameRepo sessionRepository;
    private final QuizAnswerRepository answerRepository;
    private final GamePenaltyRepository gamePenaltyRepository;
    private final EntityManager em;
    
    /**
     * 게임 결과 빌드
     */
    @Transactional(readOnly = true)
    public GameResultDto buildResult(Long sessionId) {
        log.debug("[RESULT-SERVICE] Building result for session: {}", sessionId);
        
        // 세션 확인
        GameSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND"));
        
        // Native Query로 최종 점수 집계
        String sql = """
            SELECT a.user_uid AS userUid,
                   SUM(a.score) AS totalScore,
                   SUM(CASE WHEN a.is_correct = true THEN 1 ELSE 0 END) AS correctCount,
                   COUNT(a.answer_id) AS totalAnswers
            FROM quiz_answer a
            JOIN quiz_round r ON r.round_id = a.round_id
            WHERE r.session_id = :sessionId
            GROUP BY a.user_uid
            ORDER BY totalScore DESC, correctCount DESC
            """;
        
        Query query = em.createNativeQuery(sql);
        query.setParameter("sessionId", sessionId);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        if (results.isEmpty()) {
            log.warn("[RESULT-SERVICE] No scores found for session: {}", sessionId);
            throw new UnprocessableEntityException("NO_SCORES_FOR_SESSION");
        }
        
        // PlayerScore 리스트 생성
        List<PlayerScore> scores = new ArrayList<>();
        for (Object[] row : results) {
            PlayerScore score = PlayerScore.builder()
                .userUid((String) row[0])
                .totalScore(((Number) row[1]).intValue())
                .correctCount(((Number) row[2]).intValue())
                .totalAnswers(((Number) row[3]).intValue())
                .build();
            scores.add(score);
        }
        
        // 승자 결정
        String winnerUid = scores.stream()
            .max(Comparator.comparingInt(PlayerScore::getTotalScore)
                .thenComparingInt(PlayerScore::getCorrectCount))
            .map(PlayerScore::getUserUid)
            .orElse(null);
        
        // 벌칙 정보 조회
        Long penaltyId = gamePenaltyRepository.findByGameSessionId(sessionId)
            .map(gamePenalty -> gamePenalty.getPenalty().getId())
            .orElse(null);
        
        log.info("[RESULT-SERVICE] Result built - session: {}, players: {}, winner: {}, penalty: {}",
            sessionId, scores.size(), winnerUid, penaltyId);
        
        return GameResultDto.of(sessionId, scores, winnerUid, penaltyId);
    }
    
    /**
     * 점수판 조회 (폴링용)
     */
    @Transactional(readOnly = true)
    public ScoreboardDto getScoreboard(Long sessionId) {
        // 세션 확인
        if (!sessionRepository.existsById(sessionId)) {
            throw new NotFoundException("SESSION_NOT_FOUND");
        }
        
        // 현재 점수 집계
        String sql = """
            SELECT a.user_uid AS userUid,
                   SUM(a.score) AS totalScore,
                   SUM(CASE WHEN a.is_correct = true THEN 1 ELSE 0 END) AS correctCount
            FROM quiz_answer a
            JOIN quiz_round r ON r.round_id = a.round_id
            WHERE r.session_id = :sessionId
            GROUP BY a.user_uid
            ORDER BY totalScore DESC, correctCount DESC
            """;
        
        Query query = em.createNativeQuery(sql);
        query.setParameter("sessionId", sessionId);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        List<ScoreboardDto.ScoreEntry> entries = new ArrayList<>();
        int rank = 1;
        for (Object[] row : results) {
            ScoreboardDto.ScoreEntry entry = ScoreboardDto.ScoreEntry.builder()
                .rank(rank++)
                .userUid((String) row[0])
                .score(((Number) row[1]).intValue())
                .correctCount(((Number) row[2]).intValue())
                .build();
            entries.add(entry);
        }
        
        return ScoreboardDto.builder()
            .sessionId(sessionId)
            .timestamp(System.currentTimeMillis())
            .scores(entries)
            .build();
    }
}