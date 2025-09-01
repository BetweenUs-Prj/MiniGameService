package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import su.kdt.minigame.event.LobbyEvents;
import su.kdt.minigame.config.SessionConfig;
import su.kdt.minigame.domain.*;
import su.kdt.minigame.dto.request.CreateSessionReq;
import su.kdt.minigame.dto.response.GamePenaltyResp;
import su.kdt.minigame.dto.response.SessionResp;
import su.kdt.minigame.repository.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameSessionService {

    private final QuizService quizService;
    private final ReactionGameService reactionGameService;
    private final PenaltyRepository penaltyRepository;
    private final GamePenaltyRepository gamePenaltyRepository;
    private final GameRepo gameRepo;
    private final GameSessionMemberRepo memberRepo;
    private final SessionConfig sessionConfig;
    private final SSEService sseService;
    private final su.kdt.minigame.util.PinUtil pinUtil;
    private final ApplicationEventPublisher eventPublisher;
    private final LobbyBroadcastService lobbyBroadcastService;
    private final LobbyQueryService lobbyQueryService;


    @Transactional
    public SessionResp createSession(CreateSessionReq request, String userUid) {
        Penalty selectedPenalty;
        
        if (request.penaltyId() != null) {
            selectedPenalty = penaltyRepository.findById(request.penaltyId())
                    .orElseThrow(() -> new IllegalArgumentException("Penalty not found: " + request.penaltyId()));
        } else {
            // penaltyId가 null이면 첫 번째 기본 벌칙 사용
            selectedPenalty = penaltyRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No default penalty available"));
        }

        String gameType = request.gameType();
        SessionResp response;

        if ("REACTION".equals(gameType)) {
            response = reactionGameService.createReactionSession(request, userUid, selectedPenalty);

        } else if ("QUIZ".equals(gameType)) {
            response = quizService.createQuizSession(request, userUid, selectedPenalty);

        } else {
            throw new IllegalArgumentException("지원하지 않는 게임 타입입니다: " + gameType);
        }

        return response;
    }

    /**
     * PIN 검증 (4자리)
     */
    public boolean isValidPin(String pin) {
        return pinUtil.isValidPin(pin);
    }

    /**
     * PIN 해시 생성
     */
    public String hashPin(String pin) {
        return pinUtil.hashPin(pin);
    }

    /**
     * PIN 검증
     */
    public boolean verifyPin(String plainPin, String hashedPin) {
        return pinUtil.verifyPin(plainPin, hashedPin);
    }

    // 세션 초대 코드 생성 (비공개방일 경우 PRIVATE- 접두사 추가)
    public String generateInviteCode(Long sessionId, boolean isPrivate) {
        if (isPrivate) {
            return "PRIVATE-" + sessionId;
        } else {
            return sessionId.toString();
        }
    }


    @Transactional(readOnly = true)
    public GamePenaltyResp getGamePenalty(Long sessionId) {
        GamePenalty gamePenalty = gamePenaltyRepository.findByGameSessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("아직 해당 게임의 벌칙이 결정되지 않았습니다."));
        
        return GamePenaltyResp.from(gamePenalty);
    }

    @Transactional
    public LobbyQueryService.LobbySnapshot joinByCode(String code, String userUid) {
        return joinByCode(code, userUid, null);
    }

    @Transactional
    public LobbyQueryService.LobbySnapshot joinByCode(String code, String userUid, String pin) {
        Long sessionId;
        
        // 1. 코드 접두사 처리하여 실제 세션ID 추출
        if (code != null && code.startsWith("FRIEND-")) {
            try {
                sessionId = Long.parseLong(code.substring(7)); // "FRIEND-" 제거
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid code format");
            }
        } else if (code != null && code.startsWith("PRIVATE-")) {
            try {
                sessionId = Long.parseLong(code.substring(8)); // "PRIVATE-" 제거
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid code format");
            }
        } else {
            try {
                sessionId = Long.parseLong(code);
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid code format");
            }
        }
        
        // 2. 세션 조회
        GameSession session = gameRepo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        // 2.1. 비공개방 PIN 검증
        if (Boolean.TRUE.equals(session.getIsPrivate())) {
            if (pin == null || pin.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Private room requires PIN");
            }
            if (!verifyPin(pin, session.getPinHash())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid PIN");
            }
        }

        // 2.2. 세션 상태 검증 - FINISHED 세션만 참가 불가, CANCELLED 세션은 참가 허용 (호스트가 일시적으로 떠난 경우)
        if (session.getStatus() == GameSession.Status.FINISHED) {
            throw new ResponseStatusException(HttpStatus.GONE, "GAME_FINISHED: 이미 종료된 게임입니다.");
        }
        if (session.getStatus() == GameSession.Status.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "GAME_ALREADY_STARTED: 이미 시작된 게임에는 참여할 수 없습니다.");
        }
        // CANCELLED 및 WAITING 세션은 참가 허용 - 호스트가 일시적으로 떠났거나 새로운 참가자를 위해

        // 3. 멤버 이미 존재하면 스냅샷 반환 (재접속) - 멱등성 보장
        Optional<GameSessionMember> existingMember = memberRepo.findBySessionIdAndUserUid(sessionId, userUid);
        if (existingMember.isPresent()) {
            // 이미 참가된 경우 409를 던지지 않고 현재 스냅샷을 반환 (멱등성)
            log.debug("[JOIN] User {} already in session {}, returning existing snapshot", userUid, sessionId);
            return lobbyQueryService.getLobbySnapshot(sessionId);
        }

        // 4. 현재 인원 수 조회
        long currentCount = memberRepo.countBySessionId(sessionId);
        
        // 5. 정원 초과 체크
        if (currentCount >= sessionConfig.getMaxPlayers()) {
            throw new SessionFullException(currentCount, sessionConfig.getMaxPlayers());
        }

        // 6. 멤버 추가 (중복 방지를 위해 try-catch 사용)
        try {
            GameSessionMember newMember = new GameSessionMember(sessionId, userUid);
            memberRepo.save(newMember);
            log.debug("[JOIN] Successfully added member {} to session {}", userUid, sessionId);
        } catch (Exception e) {
            // 중복 키 에러 시 재시도하지 않고 기존 데이터 반환 (race condition 처리)
            log.warn("[JOIN] Duplicate key detected for session {} user {}, returning existing snapshot: {}", sessionId, userUid, e.getMessage());
            return lobbyQueryService.getLobbySnapshot(sessionId);
        }

        // 7. 트랜잭션 커밋 후 브로드캐스트를 위한 이벤트 발행
        String gameType = session.getGameType().name();
        log.info("🚀 [JOIN-EVENT] Publishing MemberJoinedEvent - sessionId: {}, userUid: {}, gameType: {}", 
                sessionId, userUid, gameType);
        eventPublisher.publishEvent(new LobbyEvents.MemberJoinedEvent(sessionId, userUid, gameType));
        log.info("✅ [JOIN-EVENT] Event published successfully");
        
        // 8. 즉시 강제 브로드캐스트 (이벤트 리스너 실패 시 백업)
        log.info("🔥 [FORCE-SYNC] Triggering immediate lobby broadcast as fallback");
        lobbyBroadcastService.delayedBroadcast(sessionId, gameType);
        
        // 세션 상태 브로드캐스트 (참가자 입장) - 즉시 필요한 부분만
        broadcastSessionState(sessionId, "PLAYER_JOINED");
        
        // 최신 스냅샷 반환 (이벤트 리스너에서 브로드캐스트 처리)
        LobbyQueryService.LobbySnapshot snapshot = lobbyQueryService.getLobbySnapshot(sessionId);
        
        // 감사 로그
        log.info("[AUDIT] PLAYER_JOINED - sessionId: {}, userUid: {}, count: {}", sessionId, userUid, snapshot.count());
        
        // 9. 퀴즈 게임인 경우 점수판 초기화 브로드캐스트
        if (session.getGameType() == GameSession.GameType.QUIZ) {
            broadcastInitialScoreboard(sessionId);
        }

        return snapshot;
    }

    @Transactional(readOnly = true)
    public SessionLookupResp findSessionByCode(String code) {
        Long sessionId;
        
        // 코드 접두사 처리하여 실제 세션ID 추출
        if (code != null && code.startsWith("FRIEND-")) {
            try {
                sessionId = Long.parseLong(code.substring(7)); // "FRIEND-" 제거
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid code format");
            }
        } else if (code != null && code.startsWith("PRIVATE-")) {
            try {
                sessionId = Long.parseLong(code.substring(8)); // "PRIVATE-" 제거
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid code format");
            }
        } else {
            try {
                sessionId = Long.parseLong(code);
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid code format");
            }
        }
        
        GameSession session = gameRepo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        
        return new SessionLookupResp(session.getId(), session.getGameType().name(), session.getStatus().name());
    }

    @Transactional(readOnly = true)
    public GameSession getSession(Long sessionId) {
        return gameRepo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    }

    @Transactional(readOnly = true)
    public LobbyQueryService.LobbySnapshot getLobbySnapshot(Long sessionId) {
        return lobbyQueryService.getLobbySnapshot(sessionId);
    }


    public static class SessionFullException extends RuntimeException {
        private final long currentCount;
        private final int maxPlayers;
        
        public SessionFullException(long currentCount, int maxPlayers) {
            super("정원이 가득 찼습니다.");
            this.currentCount = currentCount;
            this.maxPlayers = maxPlayers;
        }
        
        public Map<String, Object> getDetails() {
            return Map.of("capacity", maxPlayers, "total", currentCount);
        }
    }

    public static class AlreadyJoinedException extends RuntimeException {
        public AlreadyJoinedException() {
            super("이미 세션에 참여중입니다.");
        }
    }

    // Response DTOs

    @Transactional
    public void leaveSession(Long sessionId, String userUid) {
        // Check if session exists
        GameSession session = gameRepo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        
        // Host leaving no longer cancels the session - just remove from member list
        // This allows hosts to refresh/navigate away without destroying the session for participants
        memberRepo.deleteBySessionIdAndUserUid(sessionId, userUid);
        
        // 트랜잭션 커밋 후 브로드캐스트를 위한 이벤트 발행
        String gameType = session.getGameType().name();
        eventPublisher.publishEvent(new LobbyEvents.MemberLeftEvent(sessionId, userUid, gameType));
        
        // 즉시 강제 브로드캐스트 (이벤트 리스너 실패 시 백업)
        log.info("🔥 [FORCE-SYNC] Triggering immediate lobby broadcast for member leave");
        lobbyBroadcastService.delayedBroadcast(sessionId, gameType);
        
        // 세션 상태 브로드캐스트 (참가자 퇴장) - 즉시 필요한 부분만
        broadcastSessionState(sessionId, "PLAYER_LEFT");
        
        // 감사 로그 (이벤트 리스너에서 처리)
        log.info("[AUDIT] PLAYER_LEFT - sessionId: {}, userUid: {}", sessionId, userUid);
    }

    @Transactional
    public void cancelSession(Long sessionId, String userUid) {
        // Check if session exists
        GameSession session = gameRepo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        
        // Only host can cancel sessions
        if (!session.getHostUid().equals(userUid)) {
            throw new NotHostException();
        }
        
        // Only WAITING sessions can be cancelled
        if (session.getStatus() != GameSession.Status.WAITING) {
            throw new InvalidStatusException();
        }
        
        // Cancel the session
        session.close();
        gameRepo.save(session);
        
        // Remove all members
        memberRepo.deleteBySessionId(sessionId);
        
        // SSE 브로드캐스트 - 세션 취소
        String gameType = session.getGameType().name().toLowerCase();
        Map<String, Object> cancelMessage = Map.of("message", "호스트가 방을 취소했습니다.");
        
        if ("quiz".equals(gameType)) {
            sseService.broadcastToQuizGame(sessionId, "session-cancelled", cancelMessage);
        } else if ("reaction".equals(gameType)) {
            sseService.broadcastToReactionGame(sessionId, "session-cancelled", cancelMessage);
        } else {
            sseService.broadcastToSession(sessionId, "session-cancelled", cancelMessage);
        }
    }

    public record MemberInfo(
            String userUid,
            boolean isReady,
            java.time.LocalDateTime joinedAt
    ) {}

    public record SessionLookupResp(
            Long sessionId,
            String gameType,
            String status
    ) {}

    @Transactional(readOnly = true)
    public MemberInfo getMemberInfo(Long sessionId, String userUid) {
        GameSessionMember member = memberRepo.findBySessionIdAndUserUid(sessionId, userUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        
        return new MemberInfo(member.getUserUid(), member.isReady(), member.getJoinedAt());
    }

    @Transactional(readOnly = true)
    public boolean isUserInSession(Long sessionId, String userUid) {
        return memberRepo.findBySessionIdAndUserUid(sessionId, userUid).isPresent();
    }
    
    // 강퇴 관련 예외 클래스들
    public static class NotHostException extends RuntimeException {
        public NotHostException() {
            super("호스트만 가능합니다.");
        }
    }
    
    public static class CannotKickHostException extends RuntimeException {
        public CannotKickHostException() {
            super("호스트는 강퇴할 수 없습니다.");
        }
    }
    
    public static class MemberNotFoundException extends RuntimeException {
        public MemberNotFoundException() {
            super("대상이 없습니다.");
        }
    }
    
    public static class NotEnoughPlayersException extends RuntimeException {
        public NotEnoughPlayersException() {
            super("2명 이상 필요합니다.");
        }
    }
    
    public static class InvalidStatusException extends RuntimeException {
        public InvalidStatusException() {
            super("이미 진행/종료된 방입니다.");
        }
    }
    
    public static class SessionCancelledException extends RuntimeException {
        private final String status;
        
        public SessionCancelledException(String status) {
            super("참가할 수 없는 상태의 세션입니다. 현재 상태: " + status);
            this.status = status;
        }
        
        public String getStatus() {
            return status;
        }
    }
    
    @Transactional
    public void kickMember(Long sessionId, String targetUid, String hostUid) {
        GameSession session = gameRepo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        
        // 호스트 권한 확인
        if (!session.getHostUid().equals(hostUid)) {
            throw new NotHostException();
        }
        
        // 호스트 자신은 강퇴 불가
        if (session.getHostUid().equals(targetUid)) {
            throw new CannotKickHostException();
        }
        
        // 멤버 존재 확인
        Optional<GameSessionMember> targetMember = memberRepo.findBySessionIdAndUserUid(sessionId, targetUid);
        if (targetMember.isEmpty()) {
            throw new MemberNotFoundException();
        }
        
        // 멤버 삭제
        memberRepo.deleteBySessionIdAndUserUid(sessionId, targetUid);
        
        // 강퇴된 사용자에게 개인 메시지 (SSE)
        sseService.sendToUser(sessionId, targetUid, "player-kicked", 
                Map.of("message", "방장에 의해 강퇴되었습니다."));
        
        // 업데이트된 멤버 목록 SSE 브로드캐스트
        LobbyQueryService.LobbySnapshot updatedSnapshot = lobbyQueryService.getLobbySnapshot(sessionId);
        String gameType = session.getGameType().name().toLowerCase();
        
        if ("quiz".equals(gameType)) {
            sseService.broadcastToQuizGame(sessionId, "members-update", updatedSnapshot.members());
        } else if ("reaction".equals(gameType)) {
            sseService.broadcastToReactionGame(sessionId, "members-update", updatedSnapshot.members());
        } else {
            sseService.broadcastToSession(sessionId, "members-update", updatedSnapshot.members());
        }
    }

    @Transactional  
    public void toggleReady(Long sessionId, String userUid) {
        GameSession session = gameRepo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        
        if (!session.getStatus().equals(GameSession.Status.WAITING)) {
            throw new InvalidStatusException();
        }
        
        GameSessionMember member = memberRepo.findBySessionIdAndUserUid(sessionId, userUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        
        member.setReady(!member.isReady());
        memberRepo.save(member);
        
        // 업데이트된 멤버 목록 SSE 브로드캐스트
        LobbyQueryService.LobbySnapshot snapshot = lobbyQueryService.getLobbySnapshot(sessionId);
        String gameType = session.getGameType().name().toLowerCase();
        
        if ("quiz".equals(gameType)) {
            sseService.broadcastToQuizGame(sessionId, "members-update", snapshot.members());
        } else if ("reaction".equals(gameType)) {
            sseService.broadcastToReactionGame(sessionId, "members-update", snapshot.members());
        } else {
            sseService.broadcastToSession(sessionId, "members-update", snapshot.members());
        }
        
        // 자동 시작 로직 제거 - 호스트가 명시적으로 시작해야 함
        log.debug("[READY] Session {} ready status updated: members.size() = {}", 
                sessionId, snapshot.count());
    }


    
    /**
     * 세션 상태를 모든 클라이언트에 브로드캐스트합니다.
     */
    private void broadcastSessionState(Long sessionId, String eventType) {
        try {
            GameSession session = gameRepo.findById(sessionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
            
            List<GameSessionMember> members = memberRepo.findBySessionIdOrderByJoinedAt(sessionId);
            
            Map<String, Object> sessionState = Map.of(
                    "sessionId", sessionId,
                    "status", session.getStatus().name(),
                    "players", members.stream().map(m -> Map.of(
                            "uid", m.getUserUid(),
                            "name", m.getUserUid().substring(0, Math.min(8, m.getUserUid().length())),
                            "isReady", m.isReady()
                    )).toList(),
                    "total", members.size()
            );
            
            Map<String, Object> message = Map.of(
                    "type", eventType,
                    "payload", sessionState
            );
            
            // 세션 상태 SSE 브로드캐스트
            sseService.broadcastToSession(sessionId, "session-state", message);
            log.info("[BROADCAST] {} for session {}: status={}, total={}", eventType, sessionId, session.getStatus(), members.size());
            
        } catch (Exception e) {
            log.error("Failed to broadcast session state: " + e.getMessage(), e);
        }
    }
    
    /**
     * 트랜잭션 커밋 후 비동기 브로드캐스트 (데이터 정합성 보장)
     */
    @Async
    public void broadcastSessionStateAsync(Long sessionId, String eventType, GameSession.Status oldStatus, GameSession.Status newStatus, long memberCount) {
        log.info("[BROADCAST-ASYNC] Triggering {} for session {}: {} -> {}, members: {}", eventType, sessionId, oldStatus, newStatus, memberCount);
        broadcastSessionState(sessionId, eventType);
    }

    /**
     * 세션 시작 - 상태 전이 단일화
     * POST /mini-games/sessions/{id}/start 에서만 호출
     */
    @Transactional
    public void startSession(Long sessionId, String hostUid) {
        GameSession session = gameRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        
        // 호스트 권한 확인
        if (!session.getHostUid().equals(hostUid)) {
            throw new IllegalStateException("Only host can start the session");
        }
        
        // 이미 시작된 경우 에러
        if (session.getStatus() != GameSession.Status.WAITING) {
            throw new IllegalStateException("Session is already started");
        }
        
        // 상태 전이: WAITING -> IN_PROGRESS
        session.setStatus(GameSession.Status.IN_PROGRESS);
        session.setStartedAt(Instant.now());
        gameRepo.save(session);
        
        log.info("[SESSION-START] Session {} started by host {}", sessionId, hostUid);
        
        // SSE 브로드캐스트
        sseService.broadcastToReactionGame(sessionId, "session-start", 
                Map.of("sessionId", sessionId, "status", "IN_PROGRESS"));
    }

    /**
     * 퀄즈 게임의 초기 점수판을 브로드캐스트합니다.
     */
    private void broadcastInitialScoreboard(Long sessionId) {
        try {
            List<GameSessionMember> members = memberRepo.findBySessionId(sessionId);
            List<Map<String, Object>> scoreboard = members.stream()
                .map(member -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("userUid", member.getUserUid());
                    row.put("nickname", member.getUserUid().substring(0, Math.min(8, member.getUserUid().length())));
                    row.put("score", 0); // 초기 점수는 0
                    return row;
                })
                .toList();
            
            sseService.broadcastToQuizGame(sessionId, "initial-scoreboard", scoreboard);
            System.out.println("[QUIZ] Initial scoreboard broadcasted for session: " + sessionId);
        } catch (Exception e) {
            System.err.println("Failed to broadcast initial scoreboard: " + e.getMessage());
        }
    }
}