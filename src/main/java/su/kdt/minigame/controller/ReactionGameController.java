package su.kdt.minigame.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.domain.ReactionResult;
import su.kdt.minigame.domain.ReactionRound;
import su.kdt.minigame.service.ReactionGameService;
import su.kdt.minigame.support.UidResolverFilter;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/mini-games/reaction")
@RequiredArgsConstructor
public class ReactionGameController {

    private final ReactionGameService reactionGameService;

    @PostMapping("/rounds")
    public ResponseEntity<Map<String, Object>> createRound(
            @RequestBody @Valid CreateRoundRequest request
    ) {
        // 동시 시작 브로드캐스트 (5초 후 시작으로 변경하여 사용자가 페이지에 도착할 시간 확보)
        reactionGameService.broadcastSimultaneousStart(request.sessionId(), 5000);
        
        ReactionRound round = reactionGameService.createRound(request.sessionId());
        
        Map<String, Object> response = Map.of(
            "roundId", round.getRoundId(),
            "status", round.getStatus(),
            "createdAt", round.getCreatedAt().toEpochMilli()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/rounds/{roundId}/click")
    public ResponseEntity<Map<String, Object>> registerClick(
            @PathVariable Long roundId,
            HttpServletRequest httpRequest
    ) {
        String userUid = (String) httpRequest.getAttribute(UidResolverFilter.ATTR_UID);
        ReactionResult result = reactionGameService.registerClick(roundId, userUid);
        
        Map<String, Object> response = Map.of(
            "resultId", result.getResultId(),
            "userUid", result.getUserUid(),
            "clickedAt", result.getClickedAt() != null ? result.getClickedAt().toEpochMilli() : 0,
            "deltaMs", result.getDeltaMs() != null ? result.getDeltaMs() : -1,
            "falseStart", result.getFalseStart(),
            "rank", result.getRankOrder() != null ? result.getRankOrder() : 0
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rounds/{roundId}")
    public ResponseEntity<Map<String, Object>> getRoundStatus(
            @PathVariable Long roundId,
            @RequestParam(required = false) Long ts // 캐시 무력화용
    ) {
        ReactionRound round = reactionGameService.getRoundStatus(roundId);
        List<ReactionResult> results = reactionGameService.getRoundResults(roundId);
        
        Map<String, Object> response = Map.of(
            "roundId", round.getRoundId(),
            "sessionId", round.getSessionId(),
            "status", round.getStatus(),
            "redAt", round.getRedAt() != null ? round.getRedAt().toEpochMilli() : 0,
            "createdAt", round.getCreatedAt().toEpochMilli(),
            "participants", results.size(),
            "results", results.stream().map(r -> Map.of(
                "userUid", r.getUserUid(),
                "deltaMs", r.getDeltaMs() != null ? r.getDeltaMs() : -1,
                "falseStart", r.getFalseStart(),
                "rank", r.getRankOrder() != null ? r.getRankOrder() : 0
            )).toList()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/{sessionId}/current-round")
    public ResponseEntity<Map<String, Object>> getCurrentRound(
            @PathVariable Long sessionId,
            @RequestParam(required = false) Long ts // 캐시 무력화용
    ) {
        ReactionRound currentRound = reactionGameService.getCurrentRound(sessionId);
        
        if (currentRound == null) {
            return ResponseEntity.noContent()
                .header("Cache-Control", "no-store, no-cache, must-revalidate")
                .build();
        }
        
        List<ReactionResult> results = reactionGameService.getRoundResults(currentRound.getRoundId());
        
        Map<String, Object> response = Map.of(
            "roundId", currentRound.getRoundId(),
            "sessionId", currentRound.getSessionId(),
            "status", currentRound.getStatus(),
            "redAt", currentRound.getRedAt() != null ? currentRound.getRedAt().toEpochMilli() : 0,
            "createdAt", currentRound.getCreatedAt().toEpochMilli(),
            "participants", results.size(),
            "results", results.stream().map(r -> Map.of(
                "userUid", r.getUserUid(),
                "deltaMs", r.getDeltaMs() != null ? r.getDeltaMs() : -1,
                "falseStart", r.getFalseStart(),
                "rank", r.getRankOrder() != null ? r.getRankOrder() : 0
            )).toList()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/{sessionId}/results")
    public ResponseEntity<Map<String, Object>> getSessionResults(@PathVariable Long sessionId) {
        try {
            Map<String, Object> results = reactionGameService.getSessionResults(sessionId);
            if (results.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 플레이어가 게임 페이지에 도착했음을 알리는 API
     */
    @PostMapping("/sessions/{sessionId}/ready")
    public ResponseEntity<Map<String, Object>> notifyPlayerReady(
            @PathVariable Long sessionId,
            HttpServletRequest httpRequest
    ) {
        String userUid = (String) httpRequest.getAttribute(UidResolverFilter.ATTR_UID);
        
        try {
            Map<String, Object> result = reactionGameService.markPlayerReady(sessionId, userUid, true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 플레이어가 게임 페이지를 떠났음을 알리는 API
     */
    @PostMapping("/sessions/{sessionId}/unready")
    public ResponseEntity<Map<String, Object>> notifyPlayerUnready(
            @PathVariable Long sessionId,
            HttpServletRequest httpRequest
    ) {
        String userUid = (String) httpRequest.getAttribute(UidResolverFilter.ATTR_UID);
        
        try {
            Map<String, Object> result = reactionGameService.markPlayerReady(sessionId, userUid, false);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    
    /**
     * HTTP 동기화 엔드포인트 - 늦게 조인한 클라이언트가 현재 상태 요청 (안전한 방어코드 포함)
     */
    @PostMapping("/sessions/{sessionId}/sync")
    public ResponseEntity<Map<String, Object>> syncGameState(
            @PathVariable Long sessionId,
            HttpServletRequest httpRequest
    ) {
        String userUid = (String) httpRequest.getAttribute(UidResolverFilter.ATTR_UID);
        
        try {
            Map<String, Object> syncResult = reactionGameService.syncGameState(sessionId, userUid);
            return ResponseEntity.ok(syncResult);
        } catch (Exception e) {
            // 에러 발생 시 기본 상태 반환 (500 에러 방지)
            return ResponseEntity.accepted().body(Map.of(
                "state", "WAITING",
                "message", "Sync request received but game not ready",
                "sessionId", sessionId
            ));
        }
    }

    /**
     * 디버그용 강제 라운드 시작 API
     */
    @PostMapping("/sessions/{sessionId}/debug/start-round")
    public ResponseEntity<Map<String, Object>> debugStartRound(@PathVariable Long sessionId) {
        try {
            reactionGameService.startReactionGame(sessionId);
            
            Map<String, Object> response = Map.of(
                "message", "Debug round started successfully",
                "sessionId", sessionId,
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to start debug round: " + e.getMessage())
            );
        }
    }

    public record CreateRoundRequest(@NotNull Long sessionId) {}
}