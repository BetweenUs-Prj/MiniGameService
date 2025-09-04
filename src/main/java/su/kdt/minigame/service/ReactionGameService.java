package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su.kdt.minigame.domain.ReactionRound;
import su.kdt.minigame.domain.ReactionResult;
import su.kdt.minigame.domain.Penalty;
import su.kdt.minigame.domain.GameSession;
import su.kdt.minigame.dto.request.CreateSessionReq;
import su.kdt.minigame.dto.response.SessionResp;
import su.kdt.minigame.repository.ReactionRoundRepo;
import su.kdt.minigame.repository.ReactionResultRepo;
import su.kdt.minigame.repository.GameRepo;
import su.kdt.minigame.repository.GameSessionMemberRepo;
import su.kdt.minigame.repository.UserRepository;
import su.kdt.minigame.domain.User;
import su.kdt.minigame.domain.GameSessionMember;


import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReactionGameService {

    private final ReactionRoundRepo reactionRoundRepo;
    private final ReactionResultRepo reactionResultRepo;
    private final SSEService sseService;
    private final TaskScheduler taskScheduler;
    private final GameRepo gameRepo;
    private final GameSessionMemberRepo memberRepo;
    private final UserRepository userRepository;

    private final su.kdt.minigame.util.PinUtil pinUtil;
    
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new HashMap<>();
    private final Map<Long, List<Long>> readyPlayers = new HashMap<>(); // sessionId -> List of ready userIds

    public SessionResp createReactionSession(CreateSessionReq request, Long userId, Penalty selectedPenalty) {
        GameSession session = new GameSession(
            request.appointmentId(),
            GameSession.GameType.valueOf(request.gameType()),
            userId,
            selectedPenalty.getPenaltyId(),
            selectedPenalty.getText(),
            1, // totalRounds = 1 for reaction games (single round per session)
            null // category not needed for reaction games
        );
        
        // 비공개방 설정
        if (Boolean.TRUE.equals(request.isPrivate())) {
            session.setIsPrivate(true);
            if (request.pin() != null && !request.pin().trim().isEmpty()) {
                session.setPinHash(pinUtil.hashPin(request.pin()));
            }
        }
        
        session = gameRepo.save(session);
        
        // Create host as first member of the session
        GameSessionMember hostMember = new GameSessionMember(session.getId(), userId);
        memberRepo.save(hostMember);
        
        return SessionResp.from(session);
    }

    @Transactional  
    public void startReactionGame(Long sessionId) {
        log.info("[REACTION-START] 🎮 startReactionGame called for session: {}", sessionId);
        
        GameSession session = gameRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        
        if (session.getGameType() != GameSession.GameType.REACTION) {
            throw new IllegalStateException("Not a reaction game session");
        }

        // 반응속도 게임 시작 로직
        log.info("[REACTION-START] 🎮 Starting reaction game for session: {}", sessionId);
        
        // 1. 즉시 첫 라운드 생성 (늦게 조인하는 사용자를 위해)
        ReactionRound initialRound = ensureActiveRound(sessionId);
        log.info("[REACTION-START] 🎯 Created initial round {} for session {}", initialRound.getRoundId(), sessionId);
        
        // 2. 동시 시작 신호 브로드캐스트 + 라운드 정보 포함
        log.info("[REACTION-START] 📡 About to broadcast round start for session {} round {}", sessionId, initialRound.getRoundId());
        broadcastRoundStart(sessionId, initialRound.getRoundId(), 1500);
        log.info("[REACTION-START] ✅ Reaction game started successfully for session: {}", sessionId);
    }

    public ReactionRound createRound(Long sessionId) {
        ReactionRound round = new ReactionRound(sessionId);
        round.setStatus("WAITING");  // 프론트엔드가 기대하는 초기 상태
        round = reactionRoundRepo.save(round);
        
        // 상태 변경은 broadcastSimultaneousStart에서 처리됨
        
        return round;
    }

    private void schedulePreparingPhase(Long roundId, long delayMs) {
        ScheduledFuture<?> task = taskScheduler.schedule(() -> {
            try {
                Optional<ReactionRound> roundOpt = reactionRoundRepo.findById(roundId);
                if (roundOpt.isPresent() && "WAITING".equals(roundOpt.get().getStatus())) {
                    ReactionRound round = roundOpt.get();
                    round.setStatus("PREPARING");
                    reactionRoundRepo.save(round);
    
                    // PREPARING 상태 브로드캐스트
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("type", "ROUND_STATE");
                    payload.put("status", "PREPARING");
                    payload.put("roundId", roundId);
    
                    sseService.broadcastToReactionGame(round.getSessionId(), "round-update", payload);
    
                    // 랜덤한 시간 후 RED 신호 발생 (1500~4000ms)
                    long redDelay = 1500 + new Random().nextInt(2500);
                    scheduleRedSignal(roundId, redDelay);
                }
            } catch (Exception e) {
                log.error("Error in schedulePreparingPhase", e);
            }
        }, Instant.now().plusMillis(delayMs));
        scheduledTasks.put(roundId, task);
    }

    private void scheduleRedSignal(Long roundId, long delayMs) {
        ScheduledFuture<?> task = taskScheduler.schedule(() -> {
            try {
                Optional<ReactionRound> roundOpt = reactionRoundRepo.findById(roundId);
                if (roundOpt.isPresent() && "PREPARING".equals(roundOpt.get().getStatus())) {
                    ReactionRound round = roundOpt.get();
                    round.setRedSignal();
                    reactionRoundRepo.save(round);
    
                    // 🔴 수정: 세션 단위 토픽 + 일관된 이벤트 스키마
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("type", "ROUND_STATE");
                    payload.put("status", "RED");
                    payload.put("roundId", roundId);
                    payload.put("redAt", round.getRedAt().toEpochMilli());
    
                    sseService.broadcastToReactionGame(round.getSessionId(), "round-update", payload);
    
                    scheduleTimeout(roundId, 10000);
                }
            } catch (Exception e) {
                log.error("Error in scheduleRedSignal", e);
            }
        }, Instant.now().plusMillis(delayMs));
        scheduledTasks.put(roundId, task);
    }
    

    public ReactionResult registerSessionClick(Long sessionId, Long userId) {
        log.info("[REACTION-CLICK] Session-based click: sessionId={}, userId={}", sessionId, userId);
        
        // 세션 기반 단판 게임에서는 세션 ID를 결과 저장에 직접 사용
        Optional<ReactionResult> existingResult = reactionResultRepo.findBySessionIdAndUserId(sessionId, userId);
        
        if (existingResult.isPresent()) {
            log.warn("[REACTION-CLICK] User {} already clicked for session {}", userId, sessionId);
            return existingResult.get(); // 중복 클릭 방지
        }
        
        // 새로운 결과 생성 (세션 기반)
        ReactionResult result = new ReactionResult(sessionId, userId);
        Instant clickTime = Instant.now();
        
        // 단판 게임이므로 즉시 결과 계산 (간단한 랜덤 지연시간)
        int deltaMs = 200 + new java.util.Random().nextInt(800); // 200-1000ms 랜덤
        result.recordClick(clickTime, deltaMs, false);
        
        ReactionResult saved = reactionResultRepo.save(result);
        
        // 모든 플레이어가 클릭했는지 확인하고 순위 계산
        checkAndCalculateRanks(sessionId);
        
        log.info("[REACTION-CLICK] Click registered: sessionId={}, userId={}, deltaMs={}ms", 
                sessionId, userId, deltaMs);
        
        return saved;
    }

    /**
     * 세션 기반 단판 게임의 순위 계산 및 게임 종료 처리
     */
    @Transactional
    public void checkAndCalculateRanks(Long sessionId) {
        log.info("[RANK-CALC] Checking ranks for session: {}", sessionId);
        
        GameSession session = gameRepo.findById(sessionId).orElse(null);
        if (session == null) {
            log.warn("[RANK-CALC] Session {} not found", sessionId);
            return;
        }
        
        // 세션의 전체 멤버 수 확인
        List<GameSessionMember> allMembers = memberRepo.findBySessionId(sessionId);
        List<ReactionResult> results = reactionResultRepo.findBySessionIdOrderByPerformance(sessionId);
        
        log.info("[RANK-CALC] Session {} - members: {}, results: {}", 
                sessionId, allMembers.size(), results.size());
        
        // 모든 플레이어가 클릭했는지 확인
        if (results.size() >= allMembers.size() && allMembers.size() >= 2) {
            log.info("[RANK-CALC] All players clicked for session {}, calculating final ranks", sessionId);
            
            // 순위 계산 (false start 우선 패널티, deltaMs 기준 정렬)
            calculateSessionRanks(results);
            
            // 게임 종료 처리
            finalizeSessionGame(sessionId);
        } else {
            log.debug("[RANK-CALC] Session {} not ready for finalization - waiting for more clicks", sessionId);
        }
    }

    /**
     * 세션 기반 단판 게임의 최종 순위 계산
     */
    private void calculateSessionRanks(List<ReactionResult> results) {
        // 정상 클릭 사용자들 먼저 순위 매기기
        List<ReactionResult> validClicks = results.stream()
            .filter(r -> !r.getFalseStart())
            .sorted((a, b) -> {
                if (a.getDeltaMs() == null && b.getDeltaMs() == null) {
                    return a.getUserId().compareTo(b.getUserId());
                }
                if (a.getDeltaMs() == null) return 1;
                if (b.getDeltaMs() == null) return -1;
                int deltaCompare = a.getDeltaMs().compareTo(b.getDeltaMs());
                return deltaCompare != 0 ? deltaCompare : a.getUserId().compareTo(b.getUserId());
            })
            .toList();

        for (int i = 0; i < validClicks.size(); i++) {
            validClicks.get(i).setRank(i + 1);
        }

        // False start 사용자들 하위 순위 매기기
        List<ReactionResult> falseStarts = results.stream()
            .filter(ReactionResult::getFalseStart)
            .sorted((a, b) -> a.getUserId().compareTo(b.getUserId()))
            .toList();

        int falseStartRank = validClicks.size() + 1;
        for (ReactionResult falseStart : falseStarts) {
            falseStart.setRank(falseStartRank++);
        }

        reactionResultRepo.saveAll(results);
    }

    /**
     * 세션 기반 단판 게임 종료 처리
     */
    @Transactional
    public void finalizeSessionGame(Long sessionId) {
        log.info("[SESSION-FINALIZE] Finalizing session-based game: {}", sessionId);
        
        GameSession session = gameRepo.findById(sessionId).orElse(null);
        if (session == null || session.getStatus() == GameSession.Status.FINISHED) {
            log.warn("[SESSION-FINALIZE] Session {} already finished or not found", sessionId);
            return;
        }
        
        // 세션 기반 결과 조회
        List<ReactionResult> allResults = reactionResultRepo.findBySessionIdOrderByPerformance(sessionId);
        
        if (allResults.isEmpty()) {
            log.warn("[SESSION-FINALIZE] No results found for session {}", sessionId);
            return;
        }
        
        // 사용자 표시명 조회
        List<Long> userIds = allResults.stream().map(ReactionResult::getUserId).toList();
        Map<Long, String> displayNameMap = userRepository.findByIdIn(userIds)
                .stream().collect(java.util.stream.Collectors.toMap(User::getId, User::getUsername));
        
        // 랭킹 구성
        List<Map<String, Object>> overallRanking = new ArrayList<>();
        for (int i = 0; i < allResults.size(); i++) {
            ReactionResult r = allResults.get(i);
            Map<String, Object> rankData = new HashMap<>();
            rankData.put("userId", r.getUserId());
            rankData.put("displayName", displayNameMap.getOrDefault(r.getUserId(), String.valueOf(r.getUserId())));
            rankData.put("deltaMs", r.getDeltaMs() != null ? r.getDeltaMs() : -1);
            rankData.put("falseStart", r.getFalseStart());
            rankData.put("rank", r.getRankOrder() != null ? r.getRankOrder() : i + 1);
            overallRanking.add(rankData);
        }
        
        Long winnerUid = allResults.get(0).getUserId();
        Long loserUid = allResults.get(allResults.size() - 1).getUserId();
        
        // 벌칙 정보 조회
        Map<String, Object> penaltyData = new HashMap<>();
        if (session.getSelectedPenaltyId() != null) {
            penaltyData.put("code", "P" + session.getSelectedPenaltyId());
            penaltyData.put("text", session.getPenaltyDescription());
        }
        
        // 최종 결과 페이로드 구성
        Map<String, Object> finalPayload = new HashMap<>();
        finalPayload.put("sessionId", sessionId);
        finalPayload.put("overallRanking", overallRanking);
        finalPayload.put("winnerUid", winnerUid);
        finalPayload.put("loserUid", loserUid);
        finalPayload.put("penalty", penaltyData);
        
        log.info("[SESSION-FINALIZE] Broadcasting final results for session {} with {} participants", 
                sessionId, overallRanking.size());
        
        // SSE 브로드캐스트
        try {
            sseService.broadcastToReactionGame(sessionId, "final-results", finalPayload);
            log.info("[SESSION-FINALIZE] Successfully broadcasted final results for session {}", sessionId);
        } catch (Exception e) {
            log.error("[SESSION-FINALIZE] Failed to broadcast final results for session {}", sessionId, e);
        }
        
        // 세션 상태를 FINISHED로 변경
        try {
            session.finish(session.getPenaltyDescription());
            gameRepo.save(session);
            log.info("[SESSION-FINALIZE] Session {} marked as FINISHED", sessionId);
            
            // 메모리 정리
            readyPlayers.remove(sessionId);
            
            // 방 닫힘 브로드캐스트
            taskScheduler.schedule(() -> {
                try {
                    sseService.broadcastToReactionGame(sessionId, "session-closed", 
                        Map.of("message", "게임이 종료되었습니다."));
                    log.info("[SESSION-FINALIZE] Broadcasted session closed message for session {}", sessionId);
                } catch (Exception e) {
                    log.error("[SESSION-FINALIZE] Failed to broadcast session closed message for session {}", sessionId, e);
                }
            }, Instant.now().plusSeconds(3));
        } catch (Exception e) {
            log.error("[SESSION-FINALIZE] Failed to mark session {} as FINISHED", sessionId, e);
        }
    }

    public ReactionResult registerClick(Long roundId, Long userId) {
        Instant clickTime = Instant.now();
        
        ReactionRound round = reactionRoundRepo.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));
        
        Long sessionId = round.getSessionId();
        
        // 이미 클릭한 사용자인지 확인 (중복 클릭 무시)
        Optional<ReactionResult> existing = reactionResultRepo.findBySessionIdAndUserId(sessionId, userId);
        if (existing.isPresent()) {
            log.info("User {} already clicked for session {}, returning existing result", userId, sessionId);
            return existing.get();
        }
        
        ReactionResult result = new ReactionResult(sessionId, userId);
        
        // FALSE START vs 정상 클릭 판정
        if (round.getRedAt() == null || clickTime.isBefore(round.getRedAt())) {
            result.recordClick(clickTime, null, true);
        } else {
            long deltaMs = Duration.between(round.getRedAt(), clickTime).toMillis();
            result.recordClick(clickTime, (int) deltaMs, false);
        }
        
        result = reactionResultRepo.save(result);
        finishRoundIfReady(roundId);
        return result;
    }

    private void scheduleTimeout(Long roundId, long timeoutMs) {
        taskScheduler.schedule(() -> {
            try {
                handleTimeoutAndFinalize(roundId);
            } catch (Exception e) {
                log.error("Error in scheduleTimeout", e);
            }
        }, Instant.now().plusMillis(timeoutMs));
    }
    
    private void handleTimeoutAndFinalize(Long roundId) {
        try {
            log.info("[TIMEOUT] Handling timeout for round {}", roundId);
            
            ReactionRound round = reactionRoundRepo.findById(roundId).orElse(null);
            if (round == null || "FINISHED".equals(round.getStatus())) {
                log.debug("[TIMEOUT] Round {} already finished or not found", roundId);
                return;
            }
            
            // 세션의 전체 멤버 수와 현재 제출자 수 확인
            Long sessionId = round.getSessionId();
            List<GameSessionMember> allMembers = memberRepo.findBySessionId(sessionId);
            List<ReactionResult> existingResults = reactionResultRepo.findByRoundIdOrderByPerformance(roundId);
            
            log.info("[TIMEOUT] Round {} - members: {}, existing results: {}", 
                    roundId, allMembers.size(), existingResults.size());
            
            // 미제출자들에게 타임아웃 결과 추가 (falseStart가 아닌 timeout으로 처리)
            Set<Long> submittedUids = existingResults.stream()
                    .map(ReactionResult::getUserId)
                    .collect(java.util.stream.Collectors.toSet());
            
            for (GameSessionMember member : allMembers) {
                if (!submittedUids.contains(member.getUserId())) {
                    try {
                        ReactionResult missedResult = new ReactionResult(roundId, member.getUserId());
                        // 타임아웃은 falseStart가 아니라 반응 시간 없음으로 처리
                        missedResult.recordClick(Instant.now(), null, false); 
                        reactionResultRepo.save(missedResult);
                        log.debug("[TIMEOUT] Added timeout result for user {} in round {}", member.getUserId(), roundId);
                    } catch (Exception e) {
                        log.error("[TIMEOUT] Failed to add timeout result for user {} in round {}", member.getUserId(), roundId, e);
                    }
                }
            }
            
            // 이제 모든 참가자 결과가 있으므로 마감 처리
            finishRoundIfReady(roundId);
            
        } catch (Exception e) {
            log.error("[TIMEOUT] Error handling timeout for round {}", roundId, e);
        }
    }

    private void finishRoundIfReady(Long roundId) {
        try {
            log.debug("[FINISH-ROUND] Checking if round {} is ready to finish", roundId);
            
            ReactionRound round = reactionRoundRepo.findById(roundId).orElse(null);
            if (round == null || "FINISHED".equals(round.getStatus())) {
                log.debug("[FINISH-ROUND] Round {} already finished or not found", roundId);
                return;
            }
            
            // 세션의 전체 멤버 수 확인
            Long sessionId = round.getSessionId();
            int expectedParticipants = (int) memberRepo.countBySessionId(sessionId);
            
            // 리액션 게임은 최소 2명 이상 참여해야 함
            if (expectedParticipants < 2) {
                log.info("[FINISH-ROUND] Round {} cannot finish - insufficient participants: {} (minimum 2 required)", 
                        roundId, expectedParticipants);
                return;
            }
            
            List<ReactionResult> results = reactionResultRepo.findByRoundIdOrderByPerformance(roundId);
            
            log.info("[FINISH-ROUND] Round {} status check - expected: {}, results: {}", roundId, expectedParticipants, results.size());
            
            // 모든 참가자가 제출했거나 타임아웃된 경우에만 마감
            if (results.size() >= expectedParticipants) {
                calculateRanks(results);
                round.finish();
                reactionRoundRepo.save(round);
                
                log.info("[FINISH-ROUND] Round {} finished with {} participants - broadcasting final results", roundId, results.size());
                
                // 세션 단위 최종 결과 브로드캐스트 (동기/비동기 모두 처리)
                finalizeAndBroadcast(round.getSessionId());
                
                ScheduledFuture<?> task = scheduledTasks.remove(roundId);
                if (task != null) {
                    task.cancel(false);
                }
            } else {
                log.debug("[FINISH-ROUND] Round {} not ready to finish - waiting for more results", roundId);
            }
            
        } catch (Exception e) {
            log.error("[FINISH-ROUND] Error finishing round {}", roundId, e);
        }
    }

    private void calculateRanks(List<ReactionResult> results) {
        // 정상 클릭 사용자들 먼저 순위 매기기
        List<ReactionResult> validClicks = results.stream()
            .filter(r -> !r.getFalseStart())
            .sorted((a, b) -> {
                if (a.getDeltaMs() == null && b.getDeltaMs() == null) {
                    return a.getUserId().compareTo(b.getUserId());
                }
                if (a.getDeltaMs() == null) return 1;
                if (b.getDeltaMs() == null) return -1;
                int deltaCompare = a.getDeltaMs().compareTo(b.getDeltaMs());
                return deltaCompare != 0 ? deltaCompare : a.getUserId().compareTo(b.getUserId());
            })
            .toList();

        for (int i = 0; i < validClicks.size(); i++) {
            validClicks.get(i).setRank(i + 1);
        }

        // False start 사용자들 하위 순위 매기기
        List<ReactionResult> falseStarts = results.stream()
            .filter(ReactionResult::getFalseStart)
            .sorted((a, b) -> a.getUserId().compareTo(b.getUserId()))
            .toList();

        int falseStartRank = validClicks.size() + 1;
        for (ReactionResult falseStart : falseStarts) {
            falseStart.setRank(falseStartRank++);
        }

        reactionResultRepo.saveAll(results);
    }
    
    public void finalizeAndBroadcast(Long sessionId) {
        try {
            log.info("[FINALIZE] Starting finalization for session {}", sessionId);
            
            // 해당 세션의 모든 라운드 결과를 조회
            List<ReactionRound> rounds = reactionRoundRepo.findBySessionId(sessionId);
            if (rounds.isEmpty()) {
                log.warn("[FINALIZE] No rounds found for session {} - cannot finalize", sessionId);
                return;
            }
            
            List<ReactionResult> allResults = reactionResultRepo.findByRoundIdIn(
                rounds.stream().map(ReactionRound::getRoundId).toList()
            );
            
            if (allResults.isEmpty()) {
                log.warn("[FINALIZE] No results found for session {} - cannot finalize", sessionId);
                return;
            }
        
        log.info("Finalizing session {} with {} results", sessionId, allResults.size());
        
        // 정렬: falseStart 우선 패널티, 그 외 deltaMs 오름차순
        allResults.sort((a, b) -> {
            if (a.getFalseStart() && !b.getFalseStart()) return 1;
            if (!a.getFalseStart() && b.getFalseStart()) return -1;
            if (a.getFalseStart() && b.getFalseStart()) {
                return a.getUserId().compareTo(b.getUserId());
            }
            // 둘 다 정상 클릭인 경우 deltaMs 비교
            if (a.getDeltaMs() == null && b.getDeltaMs() == null) {
                return a.getUserId().compareTo(b.getUserId());
            }
            if (a.getDeltaMs() == null) return 1;
            if (b.getDeltaMs() == null) return -1;
            return a.getDeltaMs().compareTo(b.getDeltaMs());
        });
        
        // 사용자 표시명 조회
        List<Long> userIds = allResults.stream().map(ReactionResult::getUserId).toList();
        Map<Long, String> displayNameMap = userRepository.findByIdIn(userIds)
                .stream().collect(java.util.stream.Collectors.toMap(User::getId, User::getUsername));
        
        // 랭크 계산
        List<Map<String, Object>> overallRanking = new ArrayList<>();
        for (int i = 0; i < allResults.size(); i++) {
            ReactionResult r = allResults.get(i);
            Map<String, Object> rankData = new HashMap<>();
            rankData.put("userId", r.getUserId());
            rankData.put("displayName", displayNameMap.getOrDefault(r.getUserId(), String.valueOf(r.getUserId())));
            rankData.put("deltaMs", r.getDeltaMs() != null ? r.getDeltaMs() : -1);
            rankData.put("falseStart", r.getFalseStart());
            rankData.put("rank", i + 1);
            overallRanking.add(rankData);
        }
        
        Long winnerUid = allResults.get(0).getUserId();
        Long loserUid = allResults.get(allResults.size() - 1).getUserId();
        
        // 벌칙 정보 조회 (세션에 저장된 값 사용)
        GameSession session = gameRepo.findById(sessionId).orElse(null);
        Map<String, Object> penaltyData = new HashMap<>();
        if (session != null && session.getSelectedPenaltyId() != null) {
            penaltyData.put("code", "P" + session.getSelectedPenaltyId());
            penaltyData.put("text", session.getPenaltyDescription());
        }
        
        // 단일 페이로드 구성
        Map<String, Object> finalPayload = new HashMap<>();
        finalPayload.put("sessionId", sessionId);
        finalPayload.put("overallRanking", overallRanking);
        finalPayload.put("winnerUid", winnerUid);
        finalPayload.put("loserUid", loserUid);
        finalPayload.put("penalty", penaltyData);
        
        log.info("Broadcasting final results to /topic/reaction/{}/final with {} rankings", 
                sessionId, overallRanking.size());
        
        // SSE 브로드캐스트 (동기적으로 확실히 전송)
        try {
            sseService.broadcastToReactionGame(sessionId, "final-results", finalPayload);
            log.info("Successfully broadcasted final results for session {}", sessionId);
        } catch (Exception e) {
            log.error("Failed to broadcast final results for session {}", sessionId, e);
        }
        
            // 세션 상태를 FINISHED로 변경
            if (session != null) {
                try {
                    session.finish(session.getPenaltyDescription());
                    gameRepo.save(session);
                    log.info("[FINALIZE] Session {} marked as FINISHED", sessionId);
                    
                    // 메모리 정리
                    readyPlayers.remove(sessionId);
                    
                    // 방 닫힘 브로드캐스트 (약간의 지연 후)
                    taskScheduler.schedule(() -> {
                        try {
                            sseService.broadcastToReactionGame(sessionId, "session-closed", 
                                Map.of("message", "게임이 종료되었습니다."));
                            log.info("[FINALIZE] Broadcasted session closed message for session {}", sessionId);
                        } catch (Exception e) {
                            log.error("[FINALIZE] Failed to broadcast session closed message for session {}", sessionId, e);
                        }
                    }, Instant.now().plusSeconds(3));
                } catch (Exception e) {
                    log.error("[FINALIZE] Failed to mark session {} as FINISHED", sessionId, e);
                }
            } else {
                log.warn("[FINALIZE] Session {} not found - cannot mark as FINISHED", sessionId);
            }
            
        } catch (Exception e) {
            log.error("[FINALIZE] Critical error in finalizeAndBroadcast for session {}", sessionId, e);
            // 비상 조치: 최소한 세션을 FINISHED 상태로 변경 시도
            try {
                GameSession session = gameRepo.findById(sessionId).orElse(null);
                if (session != null && session.getStatus() != GameSession.Status.FINISHED) {
                    session.finish(session.getPenaltyDescription());
                    gameRepo.save(session);
                    log.info("[FINALIZE] Emergency session {} marked as FINISHED", sessionId);
                }
            } catch (Exception emergencyE) {
                log.error("[FINALIZE] Emergency finalization failed for session {}", sessionId, emergencyE);
            }
        }
    }

    @Transactional(readOnly = true)
    public ReactionRound getRoundStatus(Long roundId) {
        return reactionRoundRepo.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));
    }

    @Transactional(readOnly = true)
    public List<ReactionResult> getRoundResults(Long roundId) {
        return reactionResultRepo.findByRoundIdOrderByRankOrderAsc(roundId);
    }

    @Transactional
    public ReactionRound getCurrentRound(Long sessionId) {
        GameSession session = gameRepo.findById(sessionId).orElse(null);
        if (session == null) {
            log.info("[CURRENT-ROUND] Session {} not found -> 204", sessionId);
            return null;
        }
        
        // WAITING 상태면 항상 204 (게임 시작 전)
        if (session.getStatus() == GameSession.Status.WAITING) {
            log.info("[CURRENT-ROUND] Session {} WAITING -> 204", sessionId);
            return null;
        }
        
        // FINISHED 상태면 항상 204 (게임 종료 후)
        if (session.getStatus() == GameSession.Status.FINISHED) {
            log.info("[CURRENT-ROUND] Session {} FINISHED -> 204", sessionId);
            return null;
        }
        
        // IN_PROGRESS 상태에서만 라운드 확인/생성
        if (session.getStatus() == GameSession.Status.IN_PROGRESS) {
            List<ReactionRound> activeRounds = reactionRoundRepo.findBySessionId(sessionId);
            
            // 현재 WAITING, PREPARING 또는 RED 상태인 라운드를 찾음
            ReactionRound currentRound = activeRounds.stream()
                    .filter(round -> "WAITING".equals(round.getStatus()) || "PREPARING".equals(round.getStatus()) || "RED".equals(round.getStatus()))
                    .findFirst()
                    .orElse(null);
            
            if (currentRound != null) {
                log.info("[CURRENT-ROUND] Session {} found active round {} status {}", sessionId, currentRound.getRoundId(), currentRound.getStatus());
                return currentRound;
            } else {
                // IN_PROGRESS인데 활성 라운드가 없으면 null 반환 (자동 생성 금지)
                log.info("[CURRENT-ROUND] Session {} IN_PROGRESS but no active round - host must start next round", sessionId);
                return null;
            }
        }
        
        log.info("[CURRENT-ROUND] Session {} status {} -> 204", sessionId, session.getStatus());
        return null;
    }

    /**
     * IN_PROGRESS 세션에 대해 확실히 활성 라운드가 존재하도록 보장
     * 늦게 조인하는 사용자를 위한 안전망
     */
    @Transactional
    public ReactionRound ensureActiveRound(Long sessionId) {
        List<ReactionRound> existingRounds = reactionRoundRepo.findBySessionId(sessionId);
        
        // 기존 활성 라운드가 있으면 반환
        ReactionRound activeRound = existingRounds.stream()
                .filter(round -> "WAITING".equals(round.getStatus()) || "PREPARING".equals(round.getStatus()) || "RED".equals(round.getStatus()))
                .findFirst()
                .orElse(null);
                
        if (activeRound != null) {
            log.debug("Found existing active round {} for session {}", activeRound.getRoundId(), sessionId);
            return activeRound;
        }
        
        // 활성 라운드가 없으면 새로 생성
        ReactionRound newRound = new ReactionRound(sessionId);
        newRound.setStatus("WAITING");
        newRound = reactionRoundRepo.save(newRound);
        
        // PREPARING 신호 스케줄링
        schedulePreparingPhase(newRound.getRoundId(), 2000);
        
        log.info("Ensured active round {} (WAITING) for session {}, will start PREPARING in 2000ms", 
                newRound.getRoundId(), sessionId);
        
        return newRound;
    }
    
    public void broadcastSimultaneousStart(Long sessionId, ReactionRound round, long startDelayMs) {
        Map<String, Object> startPayload = new HashMap<>();
        startPayload.put("sessionId", sessionId);
        startPayload.put("startAt", System.currentTimeMillis() + startDelayMs);
        startPayload.put("seed", new Random().nextInt(100000));
        
        Map<String, Object> rule = new HashMap<>();
        rule.put("minDelayMs", 1500);
        rule.put("maxDelayMs", 4000);
        startPayload.put("rule", rule);
        
        sseService.broadcastToReactionGame(sessionId, "game-start", startPayload);
        
        // 실제 게임 상태 변경 트리거 - 전달받은 라운드로 상태 변경 스케줄링
        if (round != null && "WAITING".equals(round.getStatus())) {
            log.info("🚀 [BROADCAST-START] Triggering game state transition for round {} with delay {}ms", 
                    round.getRoundId(), startDelayMs);
            schedulePreparingPhase(round.getRoundId(), startDelayMs);
        } else {
            log.warn("⚠️ [BROADCAST-START] Invalid round for session {}, round: {}", 
                    sessionId, round);
        }
    }

    /**
     * 라운드 시작 브로드캐스트 - 라운드 ID와 세션 정보 포함
     */
    public void broadcastRoundStart(Long sessionId, Long roundId, long startDelayMs) {
        Map<String, Object> roundStartPayload = new HashMap<>();
        roundStartPayload.put("type", "ROUND_START");
        roundStartPayload.put("sessionId", sessionId);
        roundStartPayload.put("roundId", roundId);
        roundStartPayload.put("startAt", System.currentTimeMillis() + startDelayMs);
        roundStartPayload.put("delayMs", startDelayMs);
        
        String topicPath = "/topic/reaction/" + sessionId + "/round";
        log.info("[REACTION-BROADCAST] 🎯 Broadcasting ROUND_START to {} with payload: {}", topicPath, roundStartPayload);
        
        // 세션 단위 브로드캐스트 (늦게 조인한 사용자도 수신)
        sseService.broadcastToReactionGame(sessionId, "round-start", roundStartPayload);
        log.info("[REACTION-BROADCAST] ✅ ROUND_START broadcasted successfully to session {}", sessionId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSessionResults(Long sessionId) {
        log.info("[RESULTS] Getting results for session: {}", sessionId);
        
        // 세션이 존재하는지 확인
        GameSession session = gameRepo.findById(sessionId).orElse(null);
        if (session == null) {
            log.warn("[RESULTS] Session {} not found", sessionId);
            return Map.of();
        }
        
        log.info("[RESULTS] Session {} found: {}", sessionId, session.getStatus());

        // 해당 세션의 모든 라운드 ID 조회
        List<ReactionRound> rounds = reactionRoundRepo.findBySessionId(sessionId);
        log.info("[RESULTS] Found {} rounds for session {}", rounds.size(), sessionId);
        
        if (rounds.isEmpty()) {
            log.warn("[RESULTS] No rounds found for session {}", sessionId);
            return Map.of();
        }
        
        List<Long> roundIds = rounds.stream().map(ReactionRound::getRoundId).toList();
        log.info("[RESULTS] Round IDs: {}", roundIds);
        
        // 해당 세션의 모든 라운드 결과를 조회 (실제로는 sessionId 기반으로 조회)
        List<ReactionResult> allResults = reactionResultRepo.findBySessionIdOrderByPerformance(sessionId);
        log.info("[RESULTS] Found {} results for session {}", allResults.size(), sessionId);
        
        if (allResults.isEmpty()) {
            log.warn("[RESULTS] No results found - returning empty map for session {}", sessionId);
            return Map.of();
        }
        
        log.info("[RESULTS] Processing {} results for session {}", allResults.size(), sessionId);
        
        // 결과 정렬
        allResults.sort((a, b) -> {
            if (a.getFalseStart() && !b.getFalseStart()) return 1;
            if (!a.getFalseStart() && b.getFalseStart()) return -1;
            if (a.getFalseStart() && b.getFalseStart()) {
                return a.getUserId().compareTo(b.getUserId());
            }
            if (a.getDeltaMs() == null && b.getDeltaMs() == null) {
                return a.getUserId().compareTo(b.getUserId());
            }
            if (a.getDeltaMs() == null) return 1;
            if (b.getDeltaMs() == null) return -1;
            return a.getDeltaMs().compareTo(b.getDeltaMs());
        });
        
        // 사용자 정보 조회
        List<Long> userIds = allResults.stream().map(ReactionResult::getUserId).toList();
        Map<Long, String> displayNameMap = userRepository.findByIdIn(userIds)
                .stream().collect(java.util.stream.Collectors.toMap(User::getId, User::getUsername));
        
        // 결과 구성
        List<Map<String, Object>> overallRanking = new ArrayList<>();
        for (int i = 0; i < allResults.size(); i++) {
            ReactionResult r = allResults.get(i);
            Map<String, Object> rankData = new HashMap<>();
            rankData.put("userId", r.getUserId());
            rankData.put("displayName", displayNameMap.getOrDefault(r.getUserId(), String.valueOf(r.getUserId())));
            rankData.put("deltaMs", r.getDeltaMs() != null ? r.getDeltaMs() : -1);
            rankData.put("falseStart", r.getFalseStart());
            rankData.put("rank", i + 1);
            overallRanking.add(rankData);
        }
        
        // 벌칙 정보
        Map<String, Object> penaltyData = new HashMap<>();
        if (session.getSelectedPenaltyId() != null) {
            penaltyData.put("code", "P" + session.getSelectedPenaltyId());
            penaltyData.put("text", session.getPenaltyDescription());
        }
        
        return Map.of(
            "sessionId", sessionId,
            "overallRanking", overallRanking,
            "winnerUid", allResults.get(0).getUserId(),
            "loserUid", allResults.get(allResults.size() - 1).getUserId(),
            "penalty", penaltyData
        );
    }

    /**
     * HTTP 동기화 엔드포인트용 안전한 상태 동기화
     */
    public Map<String, Object> syncGameState(Long sessionId, Long userId) {
        log.info("[SYNC] Sync request from user {} for session {}", userId, sessionId);
        
        GameSession session = gameRepo.findById(sessionId).orElseThrow(
            () -> new IllegalArgumentException("Session not found: " + sessionId)
        );
        
        // 대기 상태면 202 Accepted로 처리
        if (session.getStatus() != GameSession.Status.IN_PROGRESS) {
            log.info("[SYNC] Session {} is in {} state, returning WAITING response", 
                    sessionId, session.getStatus());
            return Map.of(
                "state", "WAITING",
                "sessionId", sessionId,
                "message", "Game not started yet"
            );
        }
        
        // 미참가 사용자 처리
        GameSessionMember member = memberRepo.findBySessionIdAndUserId(sessionId, userId).orElse(null);
        if (member == null) {
            log.warn("[SYNC] User {} is not a participant in session {}", userId, sessionId);
            return Map.of(
                "state", "ERROR",
                "sessionId", sessionId,
                "error", "NOT_PARTICIPANT",
                "message", "You are not a participant in this session"
            );
        }
        
        // IN_PROGRESS 상태에서만 라운드 동기화
        try {
            ReactionRound currentRound = reactionRoundRepo.findBySessionId(sessionId).stream()
            .filter(r -> "WAITING".equals(r.getStatus()) || "PREPARING".equals(r.getStatus()) || "RED".equals(r.getStatus()))
            .findFirst().orElse(null);
            
            if (currentRound == null) {
                log.info("[SYNC] No active round for session {}, returning IN_PROGRESS state", sessionId);
                return Map.of(
                    "state", "IN_PROGRESS",
                    "sessionId", sessionId,
                    "round", (Object) null,
                    "message", "Game in progress but no active round"
                );
            }
            
            // 정상 라운드 동기화 처리
            Map<String, Object> roundInfo = Map.of(
                "roundId", currentRound.getRoundId(),
                "status", currentRound.getStatus(),
                "createdAt", currentRound.getCreatedAt().toEpochMilli(),
                "redAt", currentRound.getRedAt() != null ? currentRound.getRedAt().toEpochMilli() : 0
            );
            
            // 브로드캐스트로 다른 클라이언트들에게도 상태 전달
            broadcastCurrentState(sessionId);
            
            log.info("[SYNC] Successfully synced session {} with round {}", sessionId, currentRound.getRoundId());
            return Map.of(
                "state", "IN_PROGRESS",
                "sessionId", sessionId,
                "round", roundInfo
            );
            
        } catch (Exception e) {
            log.error("[SYNC] Error during sync for session {}: {}", sessionId, e.getMessage(), e);
            return Map.of(
                "state", "IN_PROGRESS",
                "sessionId", sessionId,
                "round", null,
                "error", "Sync failed but game is running"
            );
        }
    }

    /**
     * 현재 상태 브로드캐스트 - 늦게 조인한 클라이언트 동기화
     */
    public void broadcastCurrentState(Long sessionId) {
        log.info("Broadcasting current state for session {}", sessionId);
        
        GameSession session = gameRepo.findById(sessionId).orElse(null);
        if (session == null) {
            log.debug("Session {} not found, skipping sync", sessionId);
            return;
        }

        // 세션 상태 정보 브로드캐스트
        List<GameSessionMember> members = memberRepo.findBySessionIdOrderByJoinedAt(sessionId);
        Map<String, Object> sessionState = Map.of(
                "sessionId", sessionId,
                "status", session.getStatus().name(),
                "players", members.stream().map(m -> Map.of(
                        "uid", m.getUserId(),
                        "name", String.valueOf(m.getUserId()).substring(0, Math.min(8, String.valueOf(m.getUserId()).length())),
                        "isReady", m.isReady()
                )).toList(),
                "total", members.size()
        );

        Map<String, Object> sessionStateMsg = Map.of(
                "type", "SESSION_SYNC",
                "payload", sessionState
        );

        // 세션 상태 브로드캐스트
        sseService.broadcastToSession(sessionId, "session-state", sessionStateMsg);

        // IN_PROGRESS 상태인 경우에만 라운드 동기화
        if (session.getStatus() == GameSession.Status.IN_PROGRESS) {
            // 현재 활성 라운드 확인 및 생성
            ReactionRound currentRound = ensureActiveRound(sessionId);
            
            Map<String, Object> syncPayload = new HashMap<>();
            syncPayload.put("type", "ROUND_SYNC");
            syncPayload.put("sessionId", sessionId);
            syncPayload.put("roundId", currentRound.getRoundId());
            syncPayload.put("status", currentRound.getStatus());
            syncPayload.put("createdAt", currentRound.getCreatedAt().toEpochMilli());
            
            if (currentRound.getRedAt() != null) {
                syncPayload.put("redAt", currentRound.getRedAt().toEpochMilli());
            }
            
            // 참가자 수 정보
            syncPayload.put("participants", members.size());
            
            // 라운드 동기화 브로드캐스트
            sseService.broadcastToReactionGame(sessionId, "round-sync", syncPayload);
            
            log.info("Broadcasted sync for session {} round {} status {}", 
                    sessionId, currentRound.getRoundId(), currentRound.getStatus());
        } else {
            log.info("Broadcasted session state sync for session {} status {}", sessionId, session.getStatus());
        }
    }

    /**
     * 플레이어가 게임 페이지에 도착했음을 표시하고 모든 플레이어가 준비되면 게임 시작
     */
    @Transactional
    public Map<String, Object> markPlayerReady(Long sessionId, Long userId, boolean ready) {
        GameSession session = gameRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        
        // 세션이 WAITING 상태가 아니면 에러
        if (session.getStatus() == GameSession.Status.FINISHED) {
            throw new IllegalStateException("Session is already finished");
        }
        
        // 해당 세션의 멤버인지 확인
        boolean isMember = memberRepo.findBySessionId(sessionId).stream()
                .anyMatch(member -> member.getUserId().equals(userId));
        
        if (!isMember) {
            throw new IllegalArgumentException("User is not a member of this session");
        }
        
        // Ready 플레이어 목록 업데이트 (in-memory와 database 동기화)
        List<Long> currentReadyPlayers = readyPlayers.computeIfAbsent(sessionId, k -> new ArrayList<>());
        
        if (ready) {
            if (!currentReadyPlayers.contains(userId)) {
                currentReadyPlayers.add(userId);
            }
        } else {
            currentReadyPlayers.remove(userId);
        }
        
        // 🔧 데이터베이스의 GameSessionMember.isReady 필드도 업데이트
        GameSessionMember member = memberRepo.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        member.setReady(ready);
        memberRepo.save(member);
        
        int totalMembers = (int) memberRepo.countBySessionId(sessionId);
        
        log.info("[REACTION] Player {} marked as {} for session {}. Ready: {}/{}", 
                userId, ready ? "ready" : "unready", sessionId, currentReadyPlayers.size(), totalMembers);
        
        // Ready 상태를 모든 클라이언트에 브로드캐스트
        Map<String, Object> readyStatus = Map.of(
                "sessionId", sessionId,
                "readyPlayers", currentReadyPlayers,
                "totalPlayers", totalMembers,
                "allReady", currentReadyPlayers.size() >= totalMembers && totalMembers >= 2
        );
        
        sseService.broadcastToReactionGame(sessionId, "ready-status", readyStatus);
        
        log.info("[REACTION] Broadcasted ready status to /topic/reaction/{}/ready: {}", sessionId, readyStatus);
        
        // 모든 플레이어가 준비되었고 최소 2명 이상이면 자동으로 게임 시작하지 않음
        // (호스트가 프론트엔드에서 시작 신호를 보낼 때까지 대기)
        
        return readyStatus;
    }

    /**
     * 플레이어가 게임 페이지에 도착했음을 표시
     */
    @Transactional
    public void markArrived(Long sessionId, Long userId) {
        markPlayerReady(sessionId, userId, true);
    }

    /**
     * 도착한 플레이어 수 반환
     */
    public int countArrived(Long sessionId) {
        List<Long> currentReadyPlayers = readyPlayers.getOrDefault(sessionId, new ArrayList<>());
        return currentReadyPlayers.size();
    }

    /**
     * 총 플레이어 수 반환
     */
    public int totalPlayers(Long sessionId) {
        return (int) memberRepo.countBySessionId(sessionId);
    }

    /**
     * 게임이 진행 중인지 확인
     */
    public boolean isInProgress(Long sessionId) {
        GameSession session = gameRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        return session.getStatus() == GameSession.Status.IN_PROGRESS;
    }

    // 수정 (레포에 existsBy... 가 없으면 stream anyMatch로 대체)
    public boolean hasActiveRound(Long sessionId) {
        return reactionRoundRepo.findBySessionId(sessionId).stream()
            .anyMatch(r -> "WAITING".equals(r.getStatus()) || "PREPARING".equals(r.getStatus()) || "RED".equals(r.getStatus()));
    }

    /**
     * 첫 라운드 시작 (상태 전이 없이)
     */
    @Transactional
    public void startFirstRound(Long sessionId) {
        log.info("[REACTION] Starting first round for session: {}", sessionId);
        
        // 중복 실행 방지를 위한 락 체크
        if (hasActiveRound(sessionId)) {
            log.warn("[REACTION] Active round already exists for session: {}", sessionId);
            return;
        }
        
        // 라운드 생성 및 시작 (상태 전이 없이)
        ReactionRound round = ensureActiveRound(sessionId);
        broadcastRoundStart(sessionId, round.getRoundId(), 1500);
        
        log.info("[REACTION] First round started successfully for session: {}", sessionId);
    }
}