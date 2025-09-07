package su.kdt.minigame.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.dto.response.GameResultDto;
import su.kdt.minigame.dto.response.ScoreboardDto;
import su.kdt.minigame.exception.NotFoundException;
import su.kdt.minigame.exception.UnprocessableEntityException;
import su.kdt.minigame.service.ResultServiceSafe;

import java.util.Map;

/**
 * 안전한 게임 결과 API (500 에러 완전 제거)
 * - 200: 결과 조회 성공
 * - 404: 세션 존재하지 않음  
 * - 410: 게임이 아직 완료되지 않음
 * - 422: 데이터 무결성 오류 (BigInteger 캐스팅 등)
 */
@Slf4j
@RestController  
@RequestMapping("/api/mini-games")
@RequiredArgsConstructor
public class ResultController {
    
    private final ResultServiceSafe resultService;
    
    /**
     * 게임 결과 조회 (완료된 세션만)
     * GET /api/mini-games/results/{sessionId}?ts={timestamp}
     */
    @GetMapping("/results/{sessionId}")
    public ResponseEntity<?> getGameResult(
            @PathVariable Long sessionId,
            @RequestParam(required = false) Long ts // 캐시 무력화용
    ) {
        try {
            log.info("[RESULT-REQ] GET /results/{}", sessionId);
            
            GameResultDto result = resultService.buildResultDto(sessionId);
            
            log.info("[RESULT-OK] sid={}, players={}, winner={}, penalty={}", 
                sessionId, result.getPlayers().size(), result.getWinnerUid(), result.getPenaltyId());
            
            // 성공 시 200 OK with no-cache headers
            return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(result);
            
        } catch (NotFoundException e) {
            // 세션 없음 -> 404 NOT_FOUND
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "status", "NOT_FOUND",
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
                ));
                
        } catch (UnprocessableEntityException e) {
            // 게임 미완료 또는 데이터 오류 -> 410 GONE 또는 422
            if ("GAME_NOT_FINISHED".equals(e.getMessage())) {
                // 게임이 아직 진행 중 -> 410 GONE
                return ResponseEntity
                    .status(HttpStatus.GONE)
                    .body(Map.of(
                        "status", "IN_PROGRESS", 
                        "message", "Game not finished yet",
                        "timestamp", System.currentTimeMillis()
                    ));
            } else {
                // 데이터 오류 -> 422 UNPROCESSABLE_ENTITY
                return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of(
                        "status", "DATA_ERROR",
                        "message", e.getMessage(),
                        "timestamp", System.currentTimeMillis()
                    ));
            }
        }
    }
    
    /**
     * 실시간 점수판 조회 (폴링용)
     * GET /api/mini-games/results/{sessionId}/scoreboard?ts={timestamp}
     */
    @GetMapping("/results/{sessionId}/scoreboard")
    public ResponseEntity<ScoreboardDto> getScoreboard(
            @PathVariable Long sessionId,
            @RequestParam(required = false) Long ts // 캐시 무력화용
    ) {
        try {
            log.debug("[SCOREBOARD-REQ] GET /results/{}/scoreboard", sessionId);
            
            ScoreboardDto scoreboard = resultService.getScoreboardSafely(sessionId);
            
            log.info("[BOARD-OK] sid={}, players={}, genTs={}", 
                sessionId, scoreboard.getScores().size(), System.currentTimeMillis());
            
            return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(scoreboard);
            
        } catch (NotFoundException e) {
            // 세션 없음 -> 404, 하지만 빈 점수판 반환 (폴링 유지)
            ScoreboardDto emptyBoard = ScoreboardDto.builder()
                .sessionId(sessionId)
                .timestamp(System.currentTimeMillis())
                .scores(java.util.Collections.emptyList())
                .build();
            return ResponseEntity.ok(emptyBoard);
        }
    }
}