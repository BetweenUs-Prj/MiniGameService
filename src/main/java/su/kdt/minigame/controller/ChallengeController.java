package su.kdt.minigame.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.domain.ChallengeEntry;
import su.kdt.minigame.domain.ChallengeGroup;
import su.kdt.minigame.dto.response.LeaderboardDto;
import su.kdt.minigame.dto.response.PresenceDto;
import su.kdt.minigame.service.ChallengeService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST 전용 Challenge 시스템
 * - 그룹 리더보드 (완료된 참가자만)
 * - Presence (현재 활동 중인 참가자)
 * - Heartbeat (last_seen_at 업데이트)
 */
@Slf4j
@RestController
@RequestMapping("/api/mini-games")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    /**
     * 그룹 리더보드 조회 (완료된 참가자만)
     * GET /api/mini-games/groups/{groupId}/leaderboard?page=0&size=20&ts={timestamp}
     */
    @GetMapping("/groups/{groupId}/leaderboard")
    public ResponseEntity<LeaderboardDto> getGroupLeaderboard(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long ts // 캐시 무력화용
    ) {
        try {
            log.debug("[LEADERBOARD-REQ] GET /groups/{}/leaderboard?page={}&size={}", groupId, page, size);
            
            Pageable pageable = PageRequest.of(page, size);
            LeaderboardDto leaderboard = challengeService.getGroupLeaderboard(groupId, pageable);
            
            log.info("[LEADERBOARD-OK] gid={}, entries={}, page={}/{}", 
                groupId, leaderboard.getEntries().size(), page, leaderboard.getTotalPages() - 1);
            
            return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(leaderboard);
                
        } catch (Exception e) {
            log.error("[LEADERBOARD-ERROR] gid={}, error={}", groupId, e.getMessage());
            return ResponseEntity.status(500)
                .body(LeaderboardDto.builder()
                    .groupId(groupId)
                    .entries(List.of())
                    .totalEntries(0)
                    .currentPage(page)
                    .totalPages(0)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    /**
     * 현재 활동 중인 참가자 조회 (Presence)
     * GET /api/mini-games/groups/{groupId}/live?ts={timestamp}
     */
    @GetMapping("/groups/{groupId}/live") 
    public ResponseEntity<PresenceDto> getLiveParticipants(
            @PathVariable Long groupId,
            @RequestParam(required = false) Long ts // 캐시 무력화용
    ) {
        try {
            log.debug("[PRESENCE-REQ] GET /groups/{}/live", groupId);
            
            PresenceDto presence = challengeService.getLiveParticipants(groupId);
            
            log.debug("[PRESENCE-OK] gid={}, active={}, ts={}", 
                groupId, presence.getActiveParticipants().size(), presence.getTimestamp());
            
            return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate") 
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(presence);
                
        } catch (Exception e) {
            log.error("[PRESENCE-ERROR] gid={}, error={}", groupId, e.getMessage());
            return ResponseEntity.status(500)
                .body(PresenceDto.builder()
                    .groupId(groupId)
                    .activeParticipants(List.of())
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    /**
     * Heartbeat 업데이트 (선택적)
     * POST /api/mini-games/sessions/{sessionId}/heartbeat
     * Body: {"roundNo": 3, "answered": 2, "elapsedMs": 45000}
     */
    @PostMapping("/sessions/{sessionId}/heartbeat")
    public ResponseEntity<?> updateHeartbeat(
            @PathVariable Long sessionId,
            @RequestHeader("User-Uid") String userUid,
            @RequestBody Map<String, Object> heartbeat
    ) {
        try {
            log.debug("[HEARTBEAT-REQ] sid={}, uid={}, hb={}", sessionId, userUid, heartbeat);
            
            challengeService.updateHeartbeat(sessionId, userUid, heartbeat);
            
            return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate")
                .body(Map.of(
                    "status", "OK",
                    "timestamp", System.currentTimeMillis()
                ));
                
        } catch (Exception e) {
            log.error("[HEARTBEAT-ERROR] sid={}, uid={}, error={}", sessionId, userUid, e.getMessage());
            return ResponseEntity.ok() // 400보다는 200으로 (heartbeat 실패해도 게임 진행에 영향 없음)
                .body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
                ));
        }
    }

    /**
     * 그룹 생성
     * POST /api/mini-games/groups
     */
    @PostMapping("/groups")
    public ResponseEntity<?> createGroup(
            @RequestHeader("User-Uid") String createdBy,
            @RequestBody Map<String, Object> request
    ) {
        try {
            Long quizId = Long.valueOf(request.get("quizId").toString());
            String title = (String) request.get("title");
            String category = (String) request.get("category");
            
            ChallengeGroup group = challengeService.createGroup(quizId, createdBy, title, category);
            
            log.info("[GROUP-CREATED] gid={}, quiz={}, creator={}, title={}", 
                group.getId(), quizId, createdBy, title);
            
            return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate")
                .body(Map.of(
                    "groupId", group.getId(),
                    "title", group.getTitle(),
                    "category", group.getCategory(),
                    "createdAt", group.getCreatedAt(),
                    "timestamp", System.currentTimeMillis()
                ));
                
        } catch (Exception e) {
            log.error("[GROUP-CREATE-ERROR] creator={}, error={}", createdBy, e.getMessage());
            return ResponseEntity.status(400)
                .body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
                ));
        }
    }

    /**
     * 그룹 참가
     * POST /api/mini-games/groups/{groupId}/join
     */
    @PostMapping("/groups/{groupId}/join")
    public ResponseEntity<?> joinGroup(
            @PathVariable Long groupId,
            @RequestHeader("User-Uid") String userUid
    ) {
        try {
            ChallengeEntry entry = challengeService.joinGroup(groupId, userUid);
            
            log.info("[GROUP-JOINED] gid={}, uid={}, entry={}", groupId, userUid, entry.getId());
            
            return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate")
                .body(Map.of(
                    "entryId", entry.getId(),
                    "groupId", groupId,
                    "userUid", userUid,
                    "status", entry.getStatus(),
                    "joinedAt", entry.getJoinedAt(),
                    "timestamp", System.currentTimeMillis()
                ));
                
        } catch (Exception e) {
            log.error("[GROUP-JOIN-ERROR] gid={}, uid={}, error={}", groupId, userUid, e.getMessage());
            return ResponseEntity.status(400)
                .body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
                ));
        }
    }
}