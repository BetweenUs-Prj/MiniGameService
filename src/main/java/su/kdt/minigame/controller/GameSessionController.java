package su.kdt.minigame.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.domain.GameSession;
import su.kdt.minigame.domain.GameSessionMember;
import su.kdt.minigame.dto.OpenSessionSummary;
import su.kdt.minigame.dto.request.CreateSessionReq;
import su.kdt.minigame.dto.response.GamePenaltyResp;
import su.kdt.minigame.dto.response.SessionResp;
import su.kdt.minigame.dto.response.ErrorResponse;
import su.kdt.minigame.exception.ErrorCode;
import su.kdt.minigame.service.GameSessionService;
import su.kdt.minigame.service.GameLifecycleService;
import su.kdt.minigame.service.LobbyQueryService;
import su.kdt.minigame.service.QuizService;
import su.kdt.minigame.dto.response.ScoreboardItem;
import su.kdt.minigame.event.LobbyEvents;

import su.kdt.minigame.repository.GameRepo;
import su.kdt.minigame.repository.GameSessionMemberRepo;
import su.kdt.minigame.service.PenaltyService;

import java.util.List;
import java.util.Map;
import su.kdt.minigame.support.UidResolverFilter;


import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@RestController
@RequestMapping("/api/mini-games/sessions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
@Slf4j
public class GameSessionController {

    private final GameSessionService gameSessionService;
    private final GameLifecycleService gameLifecycleService;
    private final LobbyQueryService lobbyQueryService;
    private final QuizService quizService;
    private final GameRepo gameRepo;
    private final GameSessionMemberRepo memberRepo;


    /**
     * 대기 중인 방 목록을 조회합니다.
     */
    @GetMapping
    public Page<OpenSessionSummary> listOpenSessions(
            @RequestParam(required = false) String gameType,
            @RequestParam(defaultValue = "WAITING") String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("[USAGE] listOpenSessions called - gameType: {}, status: {}, query: {}", gameType, status, q);
        return gameRepo.findOpenSessions(
                gameType, status, q, PageRequest.of(page, size)
        );
    }

    /**
     * 새로운 게임 세션을 생성합니다.
     */
    @PostMapping
    public ResponseEntity<SessionResp> createSession(
            HttpServletRequest httpRequest,
            @RequestBody CreateSessionReq request
    ) {
        log.info("[USAGE] createSession called - gameType: {}, category: {}", request.gameType(), request.category());
        String userUidStr = (String) httpRequest.getAttribute(UidResolverFilter.ATTR_UID);
        Long userId = Long.valueOf(userUidStr);
        SessionResp response = gameSessionService.createSession(request, userId);
        return ResponseEntity.created(URI.create("/api/mini-games/sessions/" + response.sessionId()))
                .body(response);
    }

    /**
     * 특정 게임 세션에 할당된 벌칙 결과를 조회합니다.
     */
    @GetMapping("/{sessionId}/penalty")
    public ResponseEntity<GamePenaltyResp> getPenaltyForSession(@PathVariable Long sessionId) {
        GamePenaltyResp response = gameSessionService.getGamePenalty(sessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * 세션ID를 통해 게임 세션에 참여합니다. (멱등성 보장)
     */
    @PostMapping("/{sessionId}/join")
    public ResponseEntity<?> joinBySessionId(
            @PathVariable Long sessionId,
            HttpServletRequest httpRequest
    ) {
        log.info("[USAGE] joinBySessionId called - sessionId: {}", sessionId);
        String userUidStr = (String) httpRequest.getAttribute(UidResolverFilter.ATTR_UID);
        Long userId = Long.valueOf(userUidStr);
        try {
            // sessionId로 세션을 찾고 inviteCode를 가져와서 기존 로직 재사용
            GameSession session = gameSessionService.getSession(sessionId);
            
            // 세션 상태별 로직 처리
            if (session.getStatus().equals(GameSession.Status.IN_PROGRESS)) {
                // 이미 진행 중인 게임 - 이미 참여한 사용자라면 게임으로 리다이렉트
                boolean isAlreadyMember = gameSessionService.isUserInSession(sessionId, userId);
                if (isAlreadyMember) {
                    // 진행 중인 게임에 이미 참여한 사용자 - 성공으로 처리하고 게임 상태 반환
                    LobbyQueryService.LobbySnapshot snapshot = gameSessionService.getLobbySnapshot(sessionId);
                    Map<String, Object> gameInfo = Map.of(
                        "sessionId", sessionId,
                        "status", "IN_PROGRESS",
                        "message", "이미 진행 중인 게임에 참여했습니다.",
                        "redirect", "/game/reaction/" + sessionId
                    );
                    return ResponseEntity.ok(gameInfo);
                } else {
                    Map<String, Object> errorBody = Map.of(
                            "code", "GAME_ALREADY_STARTED",
                            "message", "이미 시작된 게임에는 참여할 수 없습니다."
                    );
                    return ResponseEntity.status(409).body(errorBody);
                }
            }
            
            if (session.getStatus().equals(GameSession.Status.FINISHED)) {
                Map<String, Object> errorBody = Map.of(
                        "code", "GAME_FINISHED",
                        "message", "이미 종료된 게임입니다."
                );
                return ResponseEntity.status(410).body(errorBody); // 410 Gone
            }
            
            // 리액션 게임의 경우 IN_PROGRESS 상태에서도 참가 허용 (늦은 참가자 지원)
            if (session.getGameType() == GameSession.GameType.REACTION && 
                session.getStatus().equals(GameSession.Status.IN_PROGRESS)) {
                
                log.info("Allowing late join for reaction game - sessionId: {}, userId: {}", sessionId, userId);
                
                // 늦은 참가자는 즉시 세션에 추가
                GameSessionMember member = memberRepo.findBySessionIdAndUserId(sessionId, userId).orElse(null);
                if (member == null) {
                    member = new GameSessionMember(sessionId, userId);
                    member.setReady(true); // 게임이 이미 진행중이므로 자동으로 ready 상태
                    memberRepo.save(member);
                }
                
                LobbyQueryService.LobbySnapshot snapshot = lobbyQueryService.getLobbySnapshot(sessionId);
                log.info("🎮 [CONTROLLER] POST /join (IN_PROGRESS) - sessionId: {}, members.size(): {}, count: {}", 
                        sessionId, snapshot.members().size(), snapshot.count());
                return ResponseEntity.ok(snapshot);
            }
            
            if (!session.getStatus().equals(GameSession.Status.WAITING)) {
                Map<String, Object> errorBody = Map.of(
                        "code", "INVALID_SESSION_STATUS",
                        "message", "참가할 수 없는 상태의 세션입니다. 현재 상태: " + session.getStatus()
                );
                return ResponseEntity.status(409).body(errorBody);
            }
            
            // 초대 전용 세션인지 체크 (코드가 FRIEND- 로 시작하는지 확인)
            String inviteCode = gameSessionService.generateInviteCode(sessionId, true);
            if (inviteCode.startsWith("FRIEND-") && !session.getHostUid().equals(String.valueOf(userId))) {
                Map<String, Object> errorBody = Map.of(
                        "code", "INVITE_ONLY",
                        "message", "초대 링크로만 입장할 수 있어요"
                );
                return ResponseEntity.status(403).body(errorBody);
            }
            
            // 멱등성을 보장하는 joinByCode 호출 - 이미 참가한 경우 409가 아니라 200을 반환
            log.info("🎯 [CONTROLLER] POST /join - sessionId: {}, userId: {}", sessionId, userId);
            LobbyQueryService.LobbySnapshot oldSnapshot = gameSessionService.joinByCode(sessionId.toString(), userId);
            
            // Use new unified snapshot service for consistent response
            LobbyQueryService.LobbySnapshot snapshot = lobbyQueryService.getLobbySnapshot(sessionId);
            log.info("🎮 [CONTROLLER] POST /join SUCCESS - sessionId: {}, members.size(): {}, count: {}", 
                    sessionId, snapshot.members().size(), snapshot.count());
            return ResponseEntity.ok(snapshot);
        } catch (GameSessionService.SessionFullException e) {
            ErrorResponse errorResponse = ErrorResponse.of(
                    ErrorCode.SESSION_FULL.getCode(),
                    ErrorCode.SESSION_FULL.getMessage(),
                    e.getDetails()
            );
            return ResponseEntity.status(ErrorCode.SESSION_FULL.getStatus()).body(errorResponse);
        } catch (RuntimeException e) {
            ErrorResponse errorResponse = ErrorResponse.of(
                    ErrorCode.SESSION_NOT_FOUND.getCode(),
                    ErrorCode.SESSION_NOT_FOUND.getMessage()
            );
            return ResponseEntity.status(ErrorCode.SESSION_NOT_FOUND.getStatus()).body(errorResponse);
        }
    }

    /**
     * 초대 코드를 통해 게임 세션에 참여합니다.
     */
    @PostMapping("/join")
    public ResponseEntity<?> joinByCode(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest
    ) {
        log.info("[USAGE] joinByCode called - inviteCode: {}", request.get("inviteCode"));
        String userUidStr = (String) httpRequest.getAttribute(UidResolverFilter.ATTR_UID);
        Long userId = Long.valueOf(userUidStr);
        String code = request.get("code");
        String pin = request.get("pin"); // PIN 파라미터 추가
        try {
            LobbyQueryService.LobbySnapshot snapshot = gameSessionService.joinByCode(code, userId, pin);
            return ResponseEntity.ok(snapshot);
        } catch (GameSessionService.SessionFullException e) {
            Map<String, Object> errorBody = Map.of(
                    "code", "SESSION_FULL",
                    "message", "정원이 가득 찼습니다.",
                    "capacity", 10,
                    "total", 10
            );
            return ResponseEntity.status(409).body(errorBody);
        } catch (GameSessionService.AlreadyJoinedException e) {
            // 이미 참가된 경우 409를 던지지 않고 정상 스냅샷을 반환
            GameSessionService.SessionLookupResp lookup = gameSessionService.findSessionByCode(code);
            LobbyQueryService.LobbySnapshot snapshot = lobbyQueryService.getLobbySnapshot(lookup.sessionId());
            log.debug("[LOBBY] POST /join (AlreadyJoined) - sessionId: {}, members.size(): {}, count: {}", 
                    lookup.sessionId(), snapshot.members().size(), snapshot.count());
            return ResponseEntity.ok(snapshot);
        } catch (org.springframework.web.server.ResponseStatusException e) {
            // Handle CANCELLED/FINISHED session status errors
            if (e.getStatusCode().value() == 409) {
                Map<String, Object> errorBody = Map.of(
                        "code", "INVALID_SESSION_STATUS",
                        "message", e.getReason()
                );
                return ResponseEntity.status(409).body(errorBody);
            } else if (e.getStatusCode().value() == 410) {
                // Check if this is a reaction game that allows late join
                try {
                    GameSessionService.SessionLookupResp lookup = gameSessionService.findSessionByCode(code);
                    GameSession session = gameSessionService.getSession((long) lookup.sessionId());
                    
                    // Allow late join for reaction games in IN_PROGRESS state
                    if (session.getGameType() == GameSession.GameType.REACTION && 
                        session.getStatus().equals(GameSession.Status.IN_PROGRESS)) {
                        
                        log.info("Allowing late join for reaction game via code - sessionId: {}, userId: {}", lookup.sessionId(), userId);
                        
                        // Check if user is already a member
                        Optional<GameSessionMember> existingMember = memberRepo.findBySessionIdAndUserId((long) lookup.sessionId(), userId);
                        if (existingMember.isPresent()) {
                            log.info("User {} already joined session {}, returning lobby snapshot", userId, lookup.sessionId());
                            LobbyQueryService.LobbySnapshot snapshot = lobbyQueryService.getLobbySnapshot((long) lookup.sessionId());
                            log.debug("[LOBBY] POST /join (LateJoinExisting) - sessionId: {}, members.size(): {}, count: {}", 
                                    lookup.sessionId(), snapshot.members().size(), snapshot.count());
                            return ResponseEntity.ok(snapshot);
                        }
                        
                        // Add user as new member for late join
                        GameSessionMember newMember = new GameSessionMember((long) lookup.sessionId(), userId);
                        newMember.setReady(true); // Auto-ready for late join
                        
                        memberRepo.save(newMember);
                        log.info("User {} joined reaction game {} as late participant", userId, lookup.sessionId());
                        
                        LobbyQueryService.LobbySnapshot snapshot = lobbyQueryService.getLobbySnapshot((long) lookup.sessionId());
                        log.debug("[LOBBY] POST /join (LateJoinNew) - sessionId: {}, members.size(): {}, count: {}", 
                                lookup.sessionId(), snapshot.members().size(), snapshot.count());
                        return ResponseEntity.ok(snapshot);
                    }
                } catch (Exception lateJoinEx) {
                    log.warn("Failed to process late join for reaction game with code {}: {}", code, lateJoinEx.getMessage());
                }
                
                // Fall back to original 410 error
                Map<String, Object> errorBody = Map.of(
                        "code", "GAME_FINISHED",
                        "message", e.getReason()
                );
                return ResponseEntity.status(410).body(errorBody);
            } else if (e.getStatusCode().value() == 404) {
                Map<String, Object> errorBody = Map.of(
                        "code", "SESSION_NOT_FOUND",
                        "message", e.getReason()
                );
                return ResponseEntity.status(404).body(errorBody);
            } else if (e.getStatusCode().value() == 403) {
                Map<String, Object> errorBody = Map.of(
                        "code", "FORBIDDEN",
                        "message", e.getReason()
                );
                return ResponseEntity.status(403).body(errorBody);
            } else {
                throw e; // Re-throw for other status codes
            }
        }
    }

    /**
     * 초대 코드로 세션 정보를 조회합니다.
     */
    @GetMapping("/by-code/{code}")
    public ResponseEntity<GameSessionService.SessionLookupResp> getSessionByCode(@PathVariable String code) {
        GameSessionService.SessionLookupResp response = gameSessionService.findSessionByCode(code);
        return ResponseEntity.ok(response);
    }

    /**
     * 세션 상세 정보를 조회합니다. (참가자 목록 포함)
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable Long sessionId) {
        GameSession session = gameSessionService.getSession(sessionId);
        LobbyQueryService.LobbySnapshot lobby = gameSessionService.getLobbySnapshot(sessionId);
        
        Map<String, Object> sessionDetails = new HashMap<>();
        sessionDetails.put("sessionId", session.getId());
        sessionDetails.put("appointmentId", session.getAppointmentId());
        sessionDetails.put("gameType", session.getGameType().name());
        sessionDetails.put("status", session.getStatus().name());
        sessionDetails.put("startTime", session.getStartTime());
        sessionDetails.put("endTime", session.getEndTime());
        sessionDetails.put("category", session.getCategory());
        sessionDetails.put("totalRounds", session.getTotalRounds());
        sessionDetails.put("hostUid", session.getHostUid());
        sessionDetails.put("participants", lobby.members().stream().map(m -> Map.of(
                "userId", m.userId(),
                "isReady", m.isReady(),
                "joinedAt", m.joinedAt()
        )).toList());
        sessionDetails.put("total", lobby.total());
        sessionDetails.put("capacity", lobby.capacity());
        sessionDetails.put("inviteCode", gameSessionService.generateInviteCode(sessionId, false));
        sessionDetails.put("inviteUrl", "http://localhost:5173/lobby?sessionid=" + sessionId);
        
        return ResponseEntity.ok(sessionDetails);
    }

    /**
     * 로비 현황을 조회합니다. (통일된 스냅샷 형태 - 단일 진실원천)
     */
    @GetMapping("/{sessionId}/lobby")
    public ResponseEntity<LobbyQueryService.LobbySnapshot> getLobby(@PathVariable Long sessionId) {
        // Use single source of truth for lobby snapshots
        log.info("📄 [CONTROLLER] GET /lobby - sessionId: {}", sessionId);
        LobbyQueryService.LobbySnapshot snapshot = lobbyQueryService.getLobbySnapshot(sessionId);
        
        log.info("🎮 [CONTROLLER] GET /lobby SUCCESS - sessionId: {}, members.size(): {}, count: {}", 
                sessionId, snapshot.members().size(), snapshot.count());
        
        return ResponseEntity.ok(snapshot);
    }

    /**
     * 세션 스냅샷을 조회합니다. (프론트엔드 호환용)
     */
    @GetMapping("/{sessionId}/snapshot")
    public ResponseEntity<Map<String, Object>> getSessionSnapshot(@PathVariable Long sessionId) {
        GameSession session = gameSessionService.getSession(sessionId);
        LobbyQueryService.LobbySnapshot lobby = gameSessionService.getLobbySnapshot(sessionId);
        
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("sessionId", session.getId());
        snapshot.put("code", gameSessionService.generateInviteCode(sessionId, false));
        snapshot.put("gameType", session.getGameType().toString());
        snapshot.put("category", session.getCategory());
        snapshot.put("status", session.getStatus().toString());
        snapshot.put("maxPlayers", lobby.capacity());
        snapshot.put("memberCount", lobby.total());
        snapshot.put("members", lobby.members());
        snapshot.put("hostUid", session.getHostUid());
        snapshot.put("inviteOnly", false);
        snapshot.put("createdAt", java.time.Instant.now());
        
        // 벌칙 정보 추가 (세션에 저장된 값 사용)
        if (session.getSelectedPenaltyId() != null) {
            Map<String, Object> penaltyData = new HashMap<>();
            penaltyData.put("code", "P" + session.getSelectedPenaltyId());
            penaltyData.put("text", session.getPenaltyDescription());
            snapshot.put("penalty", penaltyData);
        }
        
        return ResponseEntity.ok(snapshot);
    }

    /**
     * 게임 세션에서 나갑니다.
     */
    @PostMapping("/{sessionId}/leave")
    public ResponseEntity<Void> leaveSession(
            @PathVariable Long sessionId,
            HttpServletRequest httpRequest
    ) {
        String userUidStr = (String) httpRequest.getAttribute(UidResolverFilter.ATTR_UID);
        Long userId = Long.valueOf(userUidStr);
        gameSessionService.leaveSession(sessionId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 게임 세션을 취소합니다. (호스트만 가능)
     */
    @PostMapping("/{sessionId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelSession(
            @PathVariable Long sessionId,
            HttpServletRequest httpRequest
    ) {
        String userUidStr = (String) httpRequest.getAttribute(UidResolverFilter.ATTR_UID);
        Long userId = Long.valueOf(userUidStr);
        
        try {
            gameSessionService.cancelSession(sessionId, userId);
            return ResponseEntity.ok(Map.of("message", "세션이 취소되었습니다."));
        } catch (GameSessionService.NotHostException e) {
            Map<String, Object> errorBody = Map.of(
                    "code", "NOT_HOST",
                    "message", "호스트만 가능합니다."
            );
            return ResponseEntity.status(403).body(errorBody);
        } catch (GameSessionService.InvalidStatusException e) {
            Map<String, Object> errorBody = Map.of(
                    "code", "INVALID_STATUS",
                    "message", "대기 중인 세션만 취소할 수 있습니다."
            );
            return ResponseEntity.status(409).body(errorBody);
        }
    }
    
    /**
     * 세션 시작 - 상태 전이 단일화
     * 호스트만 호출 가능
     * 30년차 시니어의 안정적인 게임 시작 로직
     */
    @PostMapping("/{sessionId}/start")
    public ResponseEntity<Map<String, Object>> startSession(
            @PathVariable Long sessionId,
            @RequestBody(required = false) Map<String, Object> payload,
            HttpServletRequest httpRequest
    ) {
        String hostUid = (String) httpRequest.getAttribute(UidResolverFilter.ATTR_UID);
        
        try {
            log.info("[LIFECYCLE] Starting game session: {} by host: {} with payload: {}", sessionId, hostUid, payload);
            
            // GameLifecycleService를 통한 안전한 게임 시작 (예외 기반)
            gameLifecycleService.startGame(sessionId, hostUid);
            
            log.info("[LIFECYCLE] Game session {} started successfully", sessionId);
            return ResponseEntity.ok(Map.of(
                "message", "게임이 시작되었습니다.",
                "sessionId", sessionId,
                "status", "IN_PROGRESS"
            ));
        } catch (IllegalArgumentException e) {
            log.error("[LIFECYCLE] Session not found: {}", sessionId, e);
            Map<String, Object> errorBody = Map.of(
                    "code", "SESSION_NOT_FOUND",
                    "message", "세션을 찾을 수 없습니다."
            );
            return ResponseEntity.status(404).body(errorBody);
        } catch (IllegalStateException e) {
            log.error("[LIFECYCLE] Invalid state transition for session: {}", sessionId, e);
            
            // 구체적인 에러 코드 결정
            String errorCode = "INVALID_OPERATION";
            String message = e.getMessage();
            
            if (message.contains("최소") && message.contains("명")) {
                errorCode = "INSUFFICIENT_PLAYERS";
            } else if (message.contains("준비")) {
                errorCode = "PLAYERS_NOT_READY";
            } else if (message.contains("호스트만")) {
                errorCode = "NOT_HOST";
            } else if (message.contains("상태")) {
                errorCode = "INVALID_SESSION_STATE";
            } else if (message.contains("시작할 수 없는")) {
                errorCode = "GAME_START_BLOCKED";
            }
            
            Map<String, Object> errorBody = Map.of(
                    "code", errorCode,
                    "message", message,
                    "details", Map.of(
                        "sessionId", sessionId,
                        "timestamp", java.time.Instant.now().toEpochMilli(),
                        "errorType", "VALIDATION_FAILED"
                    )
            );
            return ResponseEntity.status(409).body(errorBody);
        } catch (Exception e) {
            log.error("[LIFECYCLE] Unexpected error starting session: {}", sessionId, e);
            gameLifecycleService.handleGameError(sessionId, "GAME_START_ERROR", 
                "게임 시작 중 오류가 발생했습니다: " + e.getMessage(), true);
            
            Map<String, Object> errorBody = Map.of(
                    "code", "INTERNAL_ERROR",
                    "message", "게임 시작 중 오류가 발생했습니다."
            );
            return ResponseEntity.status(500).body(errorBody);
        }
    }

    /**
     * 멤버의 준비 상태를 토글합니다.
     */
    @PostMapping("/{sessionId}/ready")
    public ResponseEntity<Map<String, Object>> toggleReady(
            @PathVariable Long sessionId,
            HttpServletRequest httpRequest
    ) {
        String userUidStr = (String) httpRequest.getAttribute(UidResolverFilter.ATTR_UID);
        Long userId = Long.valueOf(userUidStr);
        
        try {
            gameSessionService.toggleReady(sessionId, userId);
            return ResponseEntity.ok(Map.of("message", "준비 상태가 변경되었습니다."));
        } catch (GameSessionService.InvalidStatusException e) {
            Map<String, Object> errorBody = Map.of(
                    "code", "INVALID_STATUS",
                    "message", "게임이 시작되어 준비 상태를 변경할 수 없습니다."
            );
            return ResponseEntity.status(409).body(errorBody);
        }
    }

    /**
     * 멤버 정보를 조회합니다.
     */
    @GetMapping("/{sessionId}/members/{userUid}")
    public ResponseEntity<GameSessionService.MemberInfo> getMemberInfo(
            @PathVariable Long sessionId,
            @PathVariable String userUid
    ) {
        Long userId = Long.valueOf(userUid);
        GameSessionService.MemberInfo memberInfo = gameSessionService.getMemberInfo(sessionId, userId);
        return ResponseEntity.ok(memberInfo);
    }

    /**
     * 사용자가 세션에 참여 중인지 확인합니다.
     */
    @GetMapping("/{sessionId}/members/{userUid}/exists")
    public ResponseEntity<Map<String, Boolean>> checkMemberExists(
            @PathVariable Long sessionId,
            @PathVariable String userUid
    ) {
        Long userId = Long.valueOf(userUid);
        boolean exists = gameSessionService.isUserInSession(sessionId, userId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * 방장이 멤버를 강퇴합니다.
     */
    @PostMapping("/{sessionId}/members/{targetUid}/kick")
    public ResponseEntity<Map<String, Object>> kickMember(
            @PathVariable Long sessionId,
            @PathVariable String targetUid,
            HttpServletRequest httpRequest
    ) {
        String hostUid = (String) httpRequest.getAttribute(UidResolverFilter.ATTR_UID);
        Long hostId = Long.valueOf(hostUid);
        Long targetUserId = Long.valueOf(targetUid);
        
        try {
            gameSessionService.kickMember(sessionId, targetUserId, hostId);
            return ResponseEntity.ok(Map.of("message", "강퇴되었습니다."));
        } catch (GameSessionService.NotHostException e) {
            Map<String, Object> errorBody = Map.of(
                    "code", "NOT_HOST",
                    "message", "호스트만 가능합니다."
            );
            return ResponseEntity.status(403).body(errorBody);
        } catch (GameSessionService.CannotKickHostException e) {
            Map<String, Object> errorBody = Map.of(
                    "code", "CANNOT_KICK_HOST",
                    "message", "호스트는 강퇴할 수 없습니다."
            );
            return ResponseEntity.status(409).body(errorBody);
        } catch (GameSessionService.MemberNotFoundException e) {
            Map<String, Object> errorBody = Map.of(
                    "code", "MEMBER_NOT_FOUND",
                    "message", "대상이 없습니다."
            );
            return ResponseEntity.status(404).body(errorBody);
        }
    }

    // ===================== 퀴즈 게임 전용 API =====================
    
    /**
     * [CANONICAL] 현재 활성 라운드 조회 API - 표준 경로
     * GET /api/mini-games/sessions/{sessionId}/rounds/current
     * 500 에러 절대 금지, 404/200만 반환
     */
    @GetMapping("/{sessionId}/rounds/current")
    public ResponseEntity<?> getCurrentRound(@PathVariable Long sessionId) {
        try {
            log.info("[QUIZ-API] GET /sessions/{}/rounds/current", sessionId);
            
            // 세션 존재 여부 확인
            if (!quizService.existsSession(sessionId)) {
                log.warn("[QUIZ-API] Session not found: sessionId={}", sessionId);
                return ResponseEntity.status(404)
                    .body(Map.of("code", "SESSION_NOT_FOUND", "message", "세션을 찾을 수 없습니다."));
            }
            
            // 게임 세션 타입 및 상태 확인
            GameSession session = gameSessionService.getSession(sessionId);
            if (session.getGameType() != GameSession.GameType.QUIZ) {
                log.warn("[QUIZ-API] Not a quiz session: sessionId={}, gameType={}", sessionId, session.getGameType());
                return ResponseEntity.status(422)
                    .body(Map.of("code", "INVALID_GAME_TYPE", "message", "퀴즈 세션이 아닙니다."));
            }
            
            // 현재 활성 라운드 조회
            Map<String, Object> currentRound = quizService.getCurrentRound(sessionId);
            
            if (currentRound != null) {
                log.info("[QUIZ-API] Current round found: sessionId={}, roundId={}", sessionId, currentRound.get("roundId"));
                return ResponseEntity.ok(currentRound);
            } else {
                // 라운드가 없는 경우 - 게임 상태에 따른 응답
                if (session.getStatus() == GameSession.Status.FINISHED) {
                    log.info("[QUIZ-API] Game finished: sessionId={}", sessionId);
                    return ResponseEntity.noContent()
                        .header("x-round-phase", "FINISHED")
                        .build();
                } else {
                    log.info("[QUIZ-API] Waiting for next round: sessionId={}", sessionId);
                    return ResponseEntity.noContent()
                        .header("x-round-phase", "WAITING_NEXT")
                        .build();
                }
            }
            
        } catch (Exception e) {
            log.error("[QUIZ-API] Critical error in getCurrentRound: sessionId={}", sessionId, e);
            // 에러도 대기 상태로 처리
            return ResponseEntity.noContent()
                .header("X-Round-Phase", "WAITING_NEXT")
                .build();
        }
    }
    
    /**
     * 퀴즈 라운드의 문제 정보 조회 API
     * GET /api/mini-games/quiz/rounds/{roundId}/question
     */
    @GetMapping("/quiz/rounds/{roundId}/question")
    public ResponseEntity<?> getQuestionByRoundId(@PathVariable Long roundId) {
        try {
            log.info("[QUIZ-API] GET /quiz/rounds/{}/question", roundId);
            
            // QuizService를 통해 문제 정보 조회
            Map<String, Object> question = quizService.getQuestionByRoundId(roundId);
            if (question != null) {
                log.info("[QUIZ-API] Question found: roundId={}, questionId={}", roundId, question.get("questionId"));
                return ResponseEntity.ok(question);
            } else {
                log.warn("[QUIZ-API] Question not found: roundId={}", roundId);
                return ResponseEntity.status(404)
                    .body(Map.of("code", "QUESTION_NOT_FOUND", "message", "문제를 찾을 수 없습니다."));
            }
            
        } catch (Exception e) {
            log.error("[QUIZ-API] Error getting question: roundId={}", roundId, e);
            return ResponseEntity.status(404)
                .body(Map.of("code", "QUESTION_NOT_FOUND", "message", "문제를 찾을 수 없습니다."));
        }
    }

    /**
     * 퀴즈 게임 스코어보드 조회
     * GET /api/mini-games/sessions/{sessionId}/scores
     */
    @GetMapping("/{sessionId}/scores")
    public ResponseEntity<?> getScores(@PathVariable Long sessionId) {
        try {
            log.info("[QUIZ-API] GET /sessions/{}/scores", sessionId);
            
            // 세션 존재 확인
            GameSession session = gameRepo.findById(sessionId).orElse(null);
            if (session == null) {
                return ResponseEntity.status(404)
                    .body(Map.of("code", "SESSION_NOT_FOUND", "message", "세션을 찾을 수 없습니다."));
            }
            
            // QuizService를 통해 스코어보드 조회
            List<ScoreboardItem> scoreboard = quizService.getScoreboard(sessionId);
            
            // 프론트엔드 호환성을 위한 형식으로 변환
            List<Map<String, Object>> scores = scoreboard.stream()
                .map(item -> Map.<String, Object>of(
                    "userId", item.userId(),
                    "totalScore", item.score(),
                    "score", item.score(),
                    "correctCount", item.correctCount(),
                    "totalAnswered", item.totalAnswered(),
                    "rank", item.rank()
                ))
                .toList();
            
            log.info("[QUIZ-API] Scores retrieved: sessionId={}, count={}", sessionId, scores.size());
            return ResponseEntity.ok(scores);
            
        } catch (Exception e) {
            log.error("[QUIZ-API] Error getting scores: sessionId={}", sessionId, e);
            return ResponseEntity.status(500)
                .body(Map.of("code", "INTERNAL_ERROR", "message", "스코어 조회 중 오류가 발생했습니다."));
        }
    }
}