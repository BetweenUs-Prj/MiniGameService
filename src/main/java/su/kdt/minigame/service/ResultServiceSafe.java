package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su.kdt.minigame.domain.GameSession;
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
 * 안전한 게임 결과 서비스 (500 에러 방지)
 * - NPE, ClassCast, LazyInitialization 방어
 * - BigInteger 캐스팅 오류 422 매핑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResultServiceSafe {
    
    private final GameRepo sessionRepository;
    private final QuizAnswerRepository answerRepository;
    private final GamePenaltyRepository gamePenaltyRepository;
    private final EntityManager em;
    
    /**
     * 안전한 게임 결과 빌드 (3대 크래시 원인 회피)
     */
    @Transactional(readOnly = true)
    public GameResultDto buildResultDto(Long sessionId) {
        log.info("[RESULT-REQ] sid={}", sessionId);
        
        try {
            // 1. 세션 존재 및 상태 확인
            GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND"));
            
            if (session.getStatus() != GameSession.Status.FINISHED) {
                log.info("[RESULT-ERR] sid={}, status={} (not FINISHED)", sessionId, session.getStatus());
                throw new UnprocessableEntityException("GAME_NOT_FINISHED");
            }
            
            // 2. 게임 타입에 따른 점수 집계
            List<PlayerScore> players = session.getGameType() == GameSession.GameType.REACTION ?
                aggregateReactionScoresSafely(sessionId) : aggregateFinalScoresSafely(sessionId);
            
            if (players.isEmpty()) {
                throw new UnprocessableEntityException("NO_SCORES_FOR_SESSION");
            }
            
            // 3. 승자 계산 (동점 시 정답 수 우선)
            players.sort(Comparator.comparingInt(PlayerScore::getTotalScore)
                .thenComparingInt(PlayerScore::getCorrectCount).reversed());
            
            String winnerUid = players.get(0).getUserUid();
            
            // 4. 벌칙 정보 안전 조회
            Long penaltyId = null;
            String penaltyText = null;
            try {
                var gamePenalty = gamePenaltyRepository.findByGameSessionId(sessionId);
                if (gamePenalty.isPresent()) {
                    penaltyId = gamePenalty.get().getPenalty().getId();
                    penaltyText = gamePenalty.get().getPenalty().getText();
                }
            } catch (Exception e) {
                log.warn("[RESULT-ERR] sid={}, penalty query failed: {}", sessionId, e.getMessage());
                // 벌칙 조회 실패해도 결과 반환은 계속
            }
            
            GameResultDto result = GameResultDto.builder()
                .sessionId(sessionId)
                .players(players)
                .winnerUid(winnerUid)
                .penaltyId(penaltyId)
                .penaltyText(penaltyText)
                .status("FINISHED")
                .timestamp(System.currentTimeMillis())
                .build();
            
            log.info("[RESULT-OK] sid={}, players={}, winner={}, penaltyId={}, penaltyText={}", 
                sessionId, players.size(), winnerUid, penaltyId, penaltyText);
            
            return result;
            
        } catch (NotFoundException | UnprocessableEntityException e) {
            // 이미 적절한 예외로 분류됨
            throw e;
        } catch (Exception e) {
            // 예상치 못한 오류를 422로 변환
            log.error("[RESULT-ERR] sid={}, unexpected error: {}", sessionId, e.getMessage(), e);
            throw new UnprocessableEntityException("RESULT_BUILD_FAILED");
        }
    }
    
    /**
     * 안전한 점수 집계 (BigInteger 캐스팅 오류 방지)
     */
    private List<PlayerScore> aggregateFinalScoresSafely(Long sessionId) {
        try {
            String sql = """
                SELECT CAST(a.user_id AS CHAR) AS userUid,
                       COALESCE(SUM(a.score), 0) AS totalScore,
                       COALESCE(SUM(CASE WHEN a.is_correct = true THEN 1 ELSE 0 END), 0) AS correctCnt,
                       COUNT(a.answer_id) AS totalAnswers
                FROM quiz_answer a
                JOIN quiz_round r ON r.round_id = a.round_id
                WHERE r.session_id = :sessionId
                  AND a.user_id IS NOT NULL
                GROUP BY a.user_id
                """;
            
            Query query = em.createNativeQuery(sql);
            query.setParameter("sessionId", sessionId);
            
            @SuppressWarnings("unchecked")
            List<Object[]> rows = query.getResultList();
            
            List<PlayerScore> players = new ArrayList<>();
            
            for (Object[] row : rows) {
                try {
                    PlayerScore player = PlayerScore.builder()
                        .userUid(safeStringValue(row[0]))
                        .totalScore(safeIntValue(row[1]))
                        .correctCount(safeIntValue(row[2]))
                        .totalAnswers(safeIntValue(row[3]))
                        .build();
                    players.add(player);
                } catch (Exception e) {
                    log.warn("[RESULT-ERR] sid={}, failed to parse row: {}", sessionId, e.getMessage());
                    // 개별 행 파싱 실패해도 계속
                }
            }
            
            return players;
            
        } catch (Exception e) {
            log.error("[RESULT-ERR] sid={}, query failed: {}", sessionId, e.getMessage(), e);
            throw new UnprocessableEntityException("SCORE_AGGREGATION_FAILED");
        }
    }
    
    /**
     * 반응 게임 점수 집계 (reaction_result 테이블 기반)
     */
    private List<PlayerScore> aggregateReactionScoresSafely(Long sessionId) {
        try {
            String sql = """
                SELECT CAST(rr.user_id AS CHAR) AS userUid,
                       COALESCE(SUM(
                         CASE 
                           WHEN rr.false_start = true THEN -10
                           WHEN rr.rank_order = 1 THEN 100
                           WHEN rr.rank_order = 2 THEN 50
                           WHEN rr.rank_order = 3 THEN 30
                           ELSE 10
                         END
                       ), 0) AS totalScore,
                       COALESCE(SUM(CASE WHEN rr.false_start = false AND rr.clicked_at IS NOT NULL THEN 1 ELSE 0 END), 0) AS correctCnt,
                       COUNT(rr.result_id) AS totalAnswers
                FROM reaction_result rr
                JOIN reaction_round rnd ON rnd.round_id = rr.round_id
                WHERE rnd.session_id = :sessionId
                  AND rr.user_id IS NOT NULL
                GROUP BY rr.user_id
                """;
            
            Query query = em.createNativeQuery(sql);
            query.setParameter("sessionId", sessionId);
            
            @SuppressWarnings("unchecked")
            List<Object[]> rows = query.getResultList();
            
            List<PlayerScore> players = new ArrayList<>();
            
            for (Object[] row : rows) {
                try {
                    PlayerScore player = PlayerScore.builder()
                        .userUid(safeStringValue(row[0]))
                        .totalScore(safeIntValue(row[1]))
                        .correctCount(safeIntValue(row[2]))
                        .totalAnswers(safeIntValue(row[3]))
                        .build();
                    players.add(player);
                } catch (Exception e) {
                    log.warn("[REACTION-ERR] sid={}, failed to parse row: {}", sessionId, e.getMessage());
                }
            }
            
            return players;
            
        } catch (Exception e) {
            log.error("[REACTION-ERR] sid={}, query failed: {}", sessionId, e.getMessage(), e);
            throw new UnprocessableEntityException("REACTION_AGGREGATION_FAILED");
        }
    }
    
    /**
     * 점수판 조회 (REST 폴링용)
     */
    @Transactional(readOnly = true)
    public ScoreboardDto getScoreboardSafely(Long sessionId) {
        try {
            GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND"));
            
            String sql;
            if (session.getGameType() == GameSession.GameType.REACTION) {
                sql = """
                    SELECT CAST(rr.user_id AS CHAR) AS userUid,
                           COALESCE(SUM(
                             CASE 
                               WHEN rr.false_start = true THEN -10
                               WHEN rr.rank_order = 1 THEN 100
                               WHEN rr.rank_order = 2 THEN 50
                               WHEN rr.rank_order = 3 THEN 30
                               ELSE 10
                             END
                           ), 0) AS totalScore,
                           COALESCE(SUM(CASE WHEN rr.false_start = false AND rr.clicked_at IS NOT NULL THEN 1 ELSE 0 END), 0) AS correctCnt
                    FROM reaction_result rr
                    JOIN reaction_round rnd ON rnd.round_id = rr.round_id
                    WHERE rnd.session_id = :sessionId
                      AND rr.user_id IS NOT NULL
                    GROUP BY rr.user_id
                    ORDER BY totalScore DESC, correctCnt DESC
                    """;
            } else {
                sql = """
                    SELECT CAST(a.user_id AS CHAR) AS userUid,
                           COALESCE(SUM(a.score), 0) AS totalScore,
                           COALESCE(SUM(CASE WHEN a.is_correct = true THEN 1 ELSE 0 END), 0) AS correctCnt
                    FROM quiz_answer a
                    JOIN quiz_round r ON r.round_id = a.round_id
                    WHERE r.session_id = :sessionId
                      AND a.user_id IS NOT NULL
                    GROUP BY a.user_id
                    ORDER BY totalScore DESC, correctCnt DESC
                    """;
            }
            
            Query query = em.createNativeQuery(sql);
            query.setParameter("sessionId", sessionId);
            
            @SuppressWarnings("unchecked")
            List<Object[]> rows = query.getResultList();
            
            List<ScoreboardDto.ScoreEntry> entries = new ArrayList<>();
            int rank = 1;
            
            for (Object[] row : rows) {
                try {
                    ScoreboardDto.ScoreEntry entry = ScoreboardDto.ScoreEntry.builder()
                        .rank(rank++)
                        .userUid(safeStringValue(row[0]))
                        .score(safeIntValue(row[1]))
                        .correctCount(safeIntValue(row[2]))
                        .build();
                    entries.add(entry);
                } catch (Exception e) {
                    log.warn("[SCOREBOARD-ERR] sid={}, failed to parse row: {}", sessionId, e.getMessage());
                }
            }
            
            return ScoreboardDto.builder()
                .sessionId(sessionId)
                .timestamp(System.currentTimeMillis())
                .scores(entries)
                .build();
                
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("[SCOREBOARD-ERR] sid={}, error: {}", sessionId, e.getMessage());
            // 실패해도 빈 점수판 반환 (폴링 유지)
            return ScoreboardDto.builder()
                .sessionId(sessionId)
                .timestamp(System.currentTimeMillis())
                .scores(new ArrayList<>())
                .build();
        }
    }
    
    /**
     * 안전한 문자열 변환 (null 방어)
     */
    private String safeStringValue(Object value) {
        return value != null ? value.toString() : "unknown";
    }
    
    /**
     * 안전한 정수 변환 (BigInteger/Long → int 캐스팅 오류 방지)
     */
    private int safeIntValue(Object value) {
        if (value == null) return 0;
        
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            log.warn("[RESULT] Failed to parse int value: {}", value);
            return 0;
        }
    }
}