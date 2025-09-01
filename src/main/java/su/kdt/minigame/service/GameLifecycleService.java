package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su.kdt.minigame.domain.GameSession;
import su.kdt.minigame.domain.GameSessionMember;
import su.kdt.minigame.repository.GameRepo;
import su.kdt.minigame.repository.GameSessionMemberRepo;
import su.kdt.minigame.service.QuizService;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 게임 생명주기 관리 서비스
 * 30년차 시니어의 안정적인 게임 상태 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameLifecycleService {

    private final GameRepo gameRepo;
    private final GameSessionMemberRepo memberRepo;
    private final SSEService sseService;
    private final QuizService quizService;
    
    // 세션별 상태 관리
    private final Map<Long, GameSessionState> sessionStates = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> readyPlayers = new ConcurrentHashMap<>();
    private final Map<Long, ReadWriteLock> sessionLocks = new ConcurrentHashMap<>();
    
    /**
     * 게임 세션 상태 내부 클래스
     */
    public static class GameSessionState {
        private GameSession.Status status;
        private Instant lastActivity;
        private Set<String> connectedPlayers;
        private Map<String, Object> gameData;
        private int retryCount;
        
        public GameSessionState(GameSession.Status status) {
            this.status = status;
            this.lastActivity = Instant.now();
            this.connectedPlayers = ConcurrentHashMap.newKeySet();
            this.gameData = new ConcurrentHashMap<>();
            this.retryCount = 0;
        }
        
        // Getters and setters
        public GameSession.Status getStatus() { return status; }
        public void setStatus(GameSession.Status status) { 
            this.status = status;
            this.lastActivity = Instant.now();
        }
        
        public Instant getLastActivity() { return lastActivity; }
        public Set<String> getConnectedPlayers() { return connectedPlayers; }
        public Map<String, Object> getGameData() { return gameData; }
        public int getRetryCount() { return retryCount; }
        public void incrementRetryCount() { this.retryCount++; }
        public void resetRetryCount() { this.retryCount = 0; }
    }

    /**
     * 게임 세션 초기화
     */
    @Transactional
    public void initializeSession(Long sessionId) {
        ReadWriteLock lock = sessionLocks.computeIfAbsent(sessionId, k -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        
        try {
            GameSession session = gameRepo.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
            
            GameSessionState state = new GameSessionState(session.getStatus());
            sessionStates.put(sessionId, state);
            readyPlayers.put(sessionId, ConcurrentHashMap.newKeySet());
            
            log.info("[LIFECYCLE] Session {} initialized with status {}", sessionId, session.getStatus());
            
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 플레이어 연결 등록
     */
    public void registerPlayerConnection(Long sessionId, String userUid) {
        ReadWriteLock lock = getLockForSession(sessionId);
        lock.writeLock().lock();
        
        try {
            GameSessionState state = getOrCreateSessionState(sessionId);
            state.getConnectedPlayers().add(userUid);
            
            log.info("[LIFECYCLE] Player {} connected to session {}", userUid, sessionId);
            
            // 연결 상태 브로드캐스트
            broadcastConnectionUpdate(sessionId, state);
            
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 플레이어 준비 상태 관리
     */
    public void markPlayerReady(Long sessionId, String userUid, boolean ready) {
        ReadWriteLock lock = getLockForSession(sessionId);
        lock.writeLock().lock();
        
        try {
            GameSessionState state = getOrCreateSessionState(sessionId);
            Set<String> sessionReadyPlayers = readyPlayers.get(sessionId);
            
            if (ready) {
                sessionReadyPlayers.add(userUid);
                log.info("[LIFECYCLE] Player {} marked as ready in session {}", userUid, sessionId);
            } else {
                sessionReadyPlayers.remove(userUid);
                log.info("[LIFECYCLE] Player {} marked as not ready in session {}", userUid, sessionId);
            }
            
            // 준비 상태 브로드캐스트
            broadcastReadyUpdate(sessionId, sessionReadyPlayers.size(), state.getConnectedPlayers().size());
            
            // 모든 플레이어가 준비되면 게임 시작 가능 상태로 전환
            if (sessionReadyPlayers.size() >= 2 && 
                sessionReadyPlayers.size() == state.getConnectedPlayers().size() &&
                state.getStatus() == GameSession.Status.WAITING) {
                
                log.info("[LIFECYCLE] All players ready in session {}, ready for game start", sessionId);
                broadcastAllPlayersReady(sessionId);
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 게임 시작 처리 - 예외 기반 에러 처리
     */
    @Transactional
    public void startGame(Long sessionId, String hostUid) {
        ReadWriteLock lock = getLockForSession(sessionId);
        lock.writeLock().lock();
        
        try {
            GameSession session = gameRepo.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
            
            GameSessionState state = getOrCreateSessionState(sessionId);
            
            // 🚀 멱등성 처리: 이미 시작된 게임이면 성공 응답
            if (state.getStatus() == GameSession.Status.IN_PROGRESS) {
                log.info("[LIFECYCLE] ✅ Session {} already IN_PROGRESS - idempotent success", sessionId);
                return; // 멱등: 이미 시작된 상태면 성공으로 처리
            }
            
            // 게임 시작 조건 확인  
            if (state.getStatus() != GameSession.Status.WAITING) {
                log.error("[LIFECYCLE] VALIDATION FAILED - Invalid session status: {} (expected: WAITING or IN_PROGRESS)", state.getStatus());
                throw new IllegalStateException("게임을 시작할 수 없는 상태입니다. 현재 상태: " + state.getStatus());
            }
            log.info("[LIFECYCLE] ✅ Session status check passed: {}", state.getStatus());
            
            if (!session.getHostUid().equals(hostUid)) {
                log.error("[LIFECYCLE] VALIDATION FAILED - Not host: {} vs {}", hostUid, session.getHostUid());
                throw new IllegalStateException("호스트만 게임을 시작할 수 있습니다.");
            }
            log.info("[LIFECYCLE] ✅ Host authorization check passed: {}", hostUid);
            
            Set<String> sessionReadyPlayers = readyPlayers.get(sessionId);
            if (sessionReadyPlayers == null) {
                sessionReadyPlayers = new HashSet<>();
                readyPlayers.put(sessionId, sessionReadyPlayers);
            }
            
            // 실제 DB에서 ready 상태 확인
            List<GameSessionMember> members = memberRepo.findBySessionId(sessionId);
            long readyCount = members.stream().filter(GameSessionMember::isReady).count();
            
            log.info("[LIFECYCLE] ===== GAME START VALIDATION =====");
            log.info("[LIFECYCLE] SessionId: {}", sessionId);
            log.info("[LIFECYCLE] Game Type: {}", session.getGameType());
            log.info("[LIFECYCLE] Session Status: {}", session.getStatus());
            log.info("[LIFECYCLE] Host UID: {}", session.getHostUid());
            log.info("[LIFECYCLE] Request Host UID: {}", hostUid);
            log.info("[LIFECYCLE] Total Members: {}", members.size());
            log.info("[LIFECYCLE] Ready Members: {}", readyCount);
            log.info("[LIFECYCLE] Members Details: {}", 
                    members.stream().map(m -> m.getUserUid() + "(" + (m.isReady() ? "READY" : "NOT_READY") + ")")
                           .reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b));
            
            // 1. 최소 인원 체크 (게임 타입 무관하게 2명 이상 필요)
            if (members.size() < 2) {
                log.error("[LIFECYCLE] VALIDATION FAILED - Insufficient players: {} < 2", members.size());
                throw new IllegalStateException("최소 2명이 필요합니다. 현재 " + members.size() + "명 참여중입니다.");
            }
            log.info("[LIFECYCLE] ✅ Minimum player count check passed: {} >= 2", members.size());
            
            // 2. 게임 타입별 Ready 상태 처리
            if (session.getGameType() == GameSession.GameType.QUIZ) {
                log.info("[LIFECYCLE] 🎯 QUIZ Game Type - Auto-readying all members");
                
                // 퀴즈 게임은 호스트가 모든 멤버를 자동으로 ready 상태로 변경
                int autoReadiedCount = 0;
                for (GameSessionMember member : members) {
                    if (!member.isReady()) {
                        member.setReady(true);
                        memberRepo.save(member);
                        autoReadiedCount++;
                        log.info("[LIFECYCLE] ✅ Auto-readied member: {}", member.getUserUid());
                    }
                }
                
                readyCount = members.size(); // 모든 멤버가 ready 상태가 됨
                log.info("[LIFECYCLE] ✅ Auto-readying completed - {} members updated, {} total ready", 
                        autoReadiedCount, readyCount);
                
            } else {
                log.info("[LIFECYCLE] 🎮 Non-QUIZ Game Type ({}) - Checking ready status", session.getGameType());
                
                // 리액션 게임 등은 실제 ready 상태 체크
                if (readyCount < 2) {
                    log.error("[LIFECYCLE] VALIDATION FAILED - Not enough ready players: {} ready out of {} total", 
                            readyCount, members.size());
                    throw new IllegalStateException("게임 시작을 위해 최소 2명이 준비 상태여야 합니다. 현재 " + readyCount + "명만 준비 완료.");
                }
                log.info("[LIFECYCLE] ✅ Ready status check passed: {} >= 2", readyCount);
            }
            
            log.info("[LIFECYCLE] ===== ALL VALIDATIONS PASSED =====");
            log.info("[LIFECYCLE] Final State - SessionId: {}, Members: {}, Ready: {}", 
                    sessionId, members.size(), readyCount);
            
            // 게임 시작
            session.setStatus(GameSession.Status.IN_PROGRESS);
            session.setStartedAt(Instant.now());
            gameRepo.save(session);
            
            state.setStatus(GameSession.Status.IN_PROGRESS);
            state.resetRetryCount();
            
            log.info("[LIFECYCLE] ✅ Game started successfully for session {}", sessionId);
            
            // 게임 시작 브로드캐스트
            broadcastGameStart(sessionId, session);
            
            // 🚀 퀴즈 게임인 경우 첫 번째 라운드를 같은 트랜잭션에서 즉시 생성
            if (session.getGameType() == GameSession.GameType.QUIZ) {
                final String category = session.getCategory() != null ? session.getCategory() : "GENERAL";
                try {
                    log.info("[LIFECYCLE] ⚡ Creating first quiz round atomically for session {} with category {}", sessionId, category);
                    quizService.startRound(sessionId, category);
                    log.info("[LIFECYCLE] ✅ First quiz round created atomically for session {} with category {}", sessionId, category);
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    // 🔒 중복 라운드 생성 시도 → 멱등성 처리
                    log.warn("[LIFECYCLE] ⚠️ Duplicate round creation detected for session {} - proceeding as idempotent success", sessionId);
                    // 중복 생성은 정상적인 멱등성 시나리오로 처리 (오류 아님)
                } catch (Exception e) {
                    log.error("[LIFECYCLE] ❌ Failed to create first quiz round atomically for session {}: {}", sessionId, e.getMessage(), e);
                    // 다른 예외는 실제 오류이므로 롤백
                    throw new IllegalStateException("첫 번째 퀴즈 라운드 생성에 실패했습니다: " + e.getMessage(), e);
                }
            }
            
        } catch (IllegalStateException e) {
            // 비즈니스 로직 예외는 그대로 전파
            log.error("[LIFECYCLE] Game start validation failed for session {}: {}", sessionId, e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            // 세션 없음 등의 예외
            log.error("[LIFECYCLE] Invalid argument for session {}: {}", sessionId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[LIFECYCLE] Unexpected error starting game for session {}", sessionId, e);
            handleGameError(sessionId, "GAME_START_FAILED", "게임 시작 중 예기치 못한 오류가 발생했습니다", true);
            throw new IllegalStateException("게임 시작 중 오류가 발생했습니다: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 게임 종료 처리
     */
    @Transactional
    public void finishGame(Long sessionId, Map<String, Object> results) {
        ReadWriteLock lock = getLockForSession(sessionId);
        lock.writeLock().lock();
        
        try {
            GameSession session = gameRepo.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
            
            GameSessionState state = getOrCreateSessionState(sessionId);
            
            // 게임 종료
            session.finish("게임 완료");
            gameRepo.save(session);
            
            state.setStatus(GameSession.Status.FINISHED);
            state.getGameData().put("results", results);
            
            log.info("[LIFECYCLE] Game finished for session {}", sessionId);
            
            // 게임 종료 브로드캐스트
            broadcastGameEnd(sessionId, results);
            
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 게임 상태 동기화
     */
    public Map<String, Object> syncGameState(Long sessionId, String userUid) {
        ReadWriteLock lock = getLockForSession(sessionId);
        lock.readLock().lock();
        
        try {
            GameSessionState state = sessionStates.get(sessionId);
            if (state == null) {
                // 세션 상태가 없으면 다시 초기화 시도
                initializeSession(sessionId);
                state = sessionStates.get(sessionId);
            }
            
            GameSession session = gameRepo.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
            
            Map<String, Object> syncData = new HashMap<>();
            syncData.put("sessionId", sessionId);
            syncData.put("status", state.getStatus().name());
            syncData.put("connectedPlayers", state.getConnectedPlayers().size());
            syncData.put("readyPlayers", readyPlayers.get(sessionId).size());
            syncData.put("gameType", session.getGameType().name());
            syncData.put("hostUid", session.getHostUid());
            syncData.put("gameData", state.getGameData());
            
            log.debug("[LIFECYCLE] Synced game state for session {} user {}", sessionId, userUid);
            
            return syncData;
            
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 에러 처리
     */
    public void handleGameError(Long sessionId, String errorCode, String errorMessage, boolean recoverable) {
        ReadWriteLock lock = getLockForSession(sessionId);
        lock.writeLock().lock();
        
        try {
            GameSessionState state = getOrCreateSessionState(sessionId);
            state.incrementRetryCount();
            
            Map<String, Object> errorData = Map.of(
                "code", errorCode,
                "message", errorMessage,
                "recoverable", recoverable,
                "retryCount", state.getRetryCount(),
                "timestamp", Instant.now().toEpochMilli()
            );
            
            log.error("[LIFECYCLE] Game error in session {}: {} - {}", sessionId, errorCode, errorMessage);
            
            // 에러 브로드캐스트
            broadcastGameError(sessionId, errorData);
            
            // 복구 가능한 에러이고 재시도 횟수가 임계값 미만이면 복구 시도
            if (recoverable && state.getRetryCount() <= 3) {
                attemptRecovery(sessionId);
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 복구 시도
     */
    private void attemptRecovery(Long sessionId) {
        log.info("[LIFECYCLE] Attempting recovery for session {}", sessionId);
        
        try {
            // 세션 상태 재확인 및 복구
            GameSession session = gameRepo.findById(sessionId).orElse(null);
            if (session == null) {
                log.error("[LIFECYCLE] Cannot recover - session {} not found", sessionId);
                return;
            }
            
            GameSessionState state = getOrCreateSessionState(sessionId);
            state.setStatus(session.getStatus());
            state.resetRetryCount();
            
            // 복구 완료 브로드캐스트
            Map<String, Object> recoveryData = Map.of(
                "sessionId", sessionId,
                "status", session.getStatus().name(),
                "message", "Recovery completed successfully"
            );
            
            broadcastRecoveryComplete(sessionId, recoveryData);
            
            log.info("[LIFECYCLE] Recovery completed for session {}", sessionId);
            
        } catch (Exception e) {
            log.error("[LIFECYCLE] Recovery failed for session {}", sessionId, e);
            handleGameError(sessionId, "RECOVERY_FAILED", "Failed to recover from error", false);
        }
    }

    /**
     * 브로드캐스트 메서드들
     */
    private void broadcastConnectionUpdate(Long sessionId, GameSessionState state) {
        Map<String, Object> updateData = Map.of(
            "type", "CONNECTION_UPDATE",
            "connectedPlayers", state.getConnectedPlayers().size(),
            "connectedPlayersList", new ArrayList<>(state.getConnectedPlayers())
        );
        
        sseService.broadcastToSession(sessionId, "connection-update", updateData);
    }
    
    private void broadcastReadyUpdate(Long sessionId, int readyCount, int totalPlayers) {
        Map<String, Object> readyData = Map.of(
            "type", "READY_UPDATE",
            "readyCount", readyCount,
            "total", totalPlayers,
            "allReady", readyCount >= 2 && readyCount == totalPlayers
        );
        
        // 게임 타입별 토픽에 브로드캐스트
        GameSession session = gameRepo.findById(sessionId).orElse(null);
        if (session != null) {
            String gameTypePrefix = session.getGameType().name().toLowerCase();
            if ("quiz".equals(gameTypePrefix)) {
                sseService.broadcastToQuizGame(sessionId, "ready-status", readyData);
            } else if ("reaction".equals(gameTypePrefix)) {
                sseService.broadcastToReactionGame(sessionId, "ready-status", readyData);
            } else {
                sseService.broadcastToSession(sessionId, "ready-status", readyData);
            }
        }
    }
    
    private void broadcastAllPlayersReady(Long sessionId) {
        Map<String, Object> allReadyData = Map.of(
            "type", "ALL_PLAYERS_READY",
            "message", "All players are ready, game can start"
        );
        
        sseService.broadcastToSession(sessionId, "all-players-ready", allReadyData);
    }
    
    private void broadcastGameStart(Long sessionId, GameSession session) {
        Map<String, Object> startData = Map.of(
            "type", "GAME_START",
            "sessionId", sessionId,
            "gameType", session.getGameType().name(),
            "startedAt", session.getStartedAt().toEpochMilli()
        );
        
        // 게임 타입별 SSE 브로드캐스트
        String gameTypePrefix = session.getGameType().name().toLowerCase();
        if ("quiz".equals(gameTypePrefix)) {
            sseService.broadcastToQuizGame(sessionId, "game-start", startData);
        } else if ("reaction".equals(gameTypePrefix)) {
            sseService.broadcastToReactionGame(sessionId, "game-start", startData);
        } else {
            sseService.broadcastToSession(sessionId, "game-start", startData);
        }
    }
    
    private void broadcastGameEnd(Long sessionId, Map<String, Object> results) {
        Map<String, Object> endData = Map.of(
            "type", "GAME_END",
            "sessionId", sessionId,
            "results", results,
            "finishedAt", Instant.now().toEpochMilli()
        );
        
        // 게임 타입별 SSE 브로드캐스트
        GameSession session = gameRepo.findById(sessionId).orElse(null);
        if (session != null) {
            String gameTypePrefix = session.getGameType().name().toLowerCase();
            if ("quiz".equals(gameTypePrefix)) {
                sseService.broadcastToQuizGame(sessionId, "game-end", endData);
            } else if ("reaction".equals(gameTypePrefix)) {
                sseService.broadcastToReactionGame(sessionId, "game-end", endData);
            } else {
                sseService.broadcastToSession(sessionId, "game-end", endData);
            }
        }
    }
    
    private void broadcastGameError(Long sessionId, Map<String, Object> errorData) {
        sseService.broadcastToSession(sessionId, "game-error", errorData);
        
        // 게임 타입별 SSE 브로드캐스트
        GameSession session = gameRepo.findById(sessionId).orElse(null);
        if (session != null) {
            String gameTypePrefix = session.getGameType().name().toLowerCase();
            if ("quiz".equals(gameTypePrefix)) {
                sseService.broadcastToQuizGame(sessionId, "game-error", errorData);
            } else if ("reaction".equals(gameTypePrefix)) {
                sseService.broadcastToReactionGame(sessionId, "game-error", errorData);
            }
        }
    }
    
    private void broadcastRecoveryComplete(Long sessionId, Map<String, Object> recoveryData) {
        sseService.broadcastToSession(sessionId, "recovery-complete", recoveryData);
    }

    /**
     * 유틸리티 메서드들
     */
    private ReadWriteLock getLockForSession(Long sessionId) {
        return sessionLocks.computeIfAbsent(sessionId, k -> new ReentrantReadWriteLock());
    }
    
    private GameSessionState getOrCreateSessionState(Long sessionId) {
        return sessionStates.computeIfAbsent(sessionId, k -> {
            GameSession session = gameRepo.findById(k).orElse(null);
            GameSession.Status status = session != null ? session.getStatus() : GameSession.Status.WAITING;
            return new GameSessionState(status);
        });
    }

    /**
     * 세션 정리 (세션 종료 후 호출)
     */
    public void cleanupSession(Long sessionId) {
        ReadWriteLock lock = getLockForSession(sessionId);
        lock.writeLock().lock();
        
        try {
            sessionStates.remove(sessionId);
            readyPlayers.remove(sessionId);
            sessionLocks.remove(sessionId);
            
            log.info("[LIFECYCLE] Cleaned up session {}", sessionId);
            
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 상태 조회 메서드들
     */
    public GameSession.Status getSessionStatus(Long sessionId) {
        GameSessionState state = sessionStates.get(sessionId);
        return state != null ? state.getStatus() : null;
    }
    
    public int getReadyPlayerCount(Long sessionId) {
        Set<String> sessionReadyPlayers = readyPlayers.get(sessionId);
        return sessionReadyPlayers != null ? sessionReadyPlayers.size() : 0;
    }
    
    public int getConnectedPlayerCount(Long sessionId) {
        GameSessionState state = sessionStates.get(sessionId);
        return state != null ? state.getConnectedPlayers().size() : 0;
    }
    
    public boolean isPlayerReady(Long sessionId, String userUid) {
        Set<String> sessionReadyPlayers = readyPlayers.get(sessionId);
        return sessionReadyPlayers != null && sessionReadyPlayers.contains(userUid);
    }
}