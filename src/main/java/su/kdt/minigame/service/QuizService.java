package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import su.kdt.minigame.domain.*;
import su.kdt.minigame.dto.request.CreateRoundReq;
import su.kdt.minigame.dto.request.CreateSessionReq;
import su.kdt.minigame.dto.request.SubmitAnswerReq;
import su.kdt.minigame.dto.response.AnswerResp;
import su.kdt.minigame.dto.response.GameResultsResp;
import su.kdt.minigame.dto.response.QuizQuestionResp;
import su.kdt.minigame.dto.response.RoundResp;
import su.kdt.minigame.dto.response.ScoreboardItem;
import su.kdt.minigame.dto.response.SessionResp;
import su.kdt.minigame.repository.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private record UserScore(String userUid, long correctAnswers, long totalTime) {}

    private final GameRepo gameRepo;
    private final QuizRoundRepo roundRepo;
    private final QuizAnswerRepository answerRepo;
    private final QuizQuestionRepo questionRepo;
    private final QuizQuestionOptionRepo optionRepo;
    private final GamePenaltyRepository gamePenaltyRepository;
    private final PenaltyRepository penaltyRepository;
    private final GameSessionMemberRepo memberRepo;
    private final SSEService sseService;
    private final su.kdt.minigame.util.PinUtil pinUtil;

    @Transactional
    public SessionResp createQuizSession(CreateSessionReq req, String userUid, Penalty selectedPenalty) {
        final int DEFAULT_ROUNDS = 5;
        Integer totalRounds = (req.totalRounds() != null && req.totalRounds() > 0)
                ? req.totalRounds()
                : DEFAULT_ROUNDS;
        
        // 카테고리 기본값 설정
        String category = (req.category() != null && !req.category().trim().isEmpty())
                ? req.category()
                : "상식"; // 기본값은 상식

        GameSession session = new GameSession(req.appointmentId(), GameSession.GameType.QUIZ, userUid, selectedPenalty.getPenaltyId(), selectedPenalty.getText(), totalRounds, category);
        
        log.info("[SESSION] create id=will_be_generated, category={}, rounds={}", category, totalRounds);
        
        // 비공개방 설정
        if (Boolean.TRUE.equals(req.isPrivate())) {
            session.setIsPrivate(true);
            if (req.pin() != null && !req.pin().trim().isEmpty()) {
                session.setPinHash(pinUtil.hashPin(req.pin()));
            }
        }
        
        GameSession savedSession = gameRepo.save(session);
        
        log.info("[SESSION] create id={}, category={}, rounds={}", savedSession.getId(), category, totalRounds);
        
        // Create host as first member of the session
        GameSessionMember hostMember = new GameSessionMember(savedSession.getId(), userUid);
        memberRepo.save(hostMember);
        
        return SessionResp.from(savedSession);
    }

    @Transactional
    public RoundResp startFirstRound(Long sessionId) {
        log.info("[QUIZ] Starting first round for sessionId: {}", sessionId);
        GameSession session = findSession(sessionId);
        if (session.getGameType() != GameSession.GameType.QUIZ) {
            throw new IllegalStateException("Not a quiz session");
        }

        // 기본 카테고리로 첫 번째 라운드 시작 (세션 카테고리 사용)
        String sessionCategory = session.getCategory();
        log.info("[QUIZ] Using session category: {} for sessionId: {}", sessionCategory, sessionId);
        return startRound(sessionId, sessionCategory);
    }

    @Transactional
    public RoundResp startRoundWithQuestion(Long sessionId, CreateRoundReq req) {
        GameSession session = findSession(sessionId);
        if (session.getGameType() != GameSession.GameType.QUIZ) {
            throw new IllegalStateException("Not a quiz session");
        }

        session.start();

        QuizQuestion question = findQuestion(req.questionId());
        QuizRound round = new QuizRound(sessionId, question);
        QuizRound savedRound = roundRepo.save(round);

        // Send WebSocket notification - 통일된 topic 패턴 사용
        String roundTopic = "/topic/quiz/" + sessionId + "/round";
        Map<String, Object> roundPayload = Map.of(
                "type", "ROUND_START",
                "data", Map.of(
                    "roundId", savedRound.getRoundId(),
                    "roundNo", savedRound.getRoundNo(), 
                    "question", QuizQuestionResp.from(question)
                )
        );
        sseService.broadcastToQuizGame(sessionId, "round-start", roundPayload);
        
        // 점수판 초기화 브로드캐스트 (afterCommit 패턴)
        afterCommit(() -> broadcastScoreboard(sessionId));

        return RoundResp.from(savedRound);
    }

    @Transactional(readOnly = true)
    public Page<QuizQuestionResp> getQuestions(Long placeId, String category, Pageable pageable) {
        Page<QuizQuestion> questions = questionRepo.search(placeId, category, pageable);
        return questions.map(QuizQuestionResp::from);
    }

    @Transactional(readOnly = true)
    public QuizQuestionResp getSessionQuestion(Long sessionId, String category) {
        // 세션 존재 확인
        GameSession session = findSession(sessionId);
        if (session.getGameType() != GameSession.GameType.QUIZ) {
            throw new IllegalStateException("Not a quiz session");
        }

        // 랜덤 질문 조회 (카테고리 지정 시 해당 카테고리에서, 아니면 전체에서)
        QuizQuestion question = findQuestion(null, category);
        return QuizQuestionResp.from(question);
    }

    @Transactional
    public AnswerResp submitAnswer(Long roundId, SubmitAnswerReq req) {
        log.info("[QUIZ] submitAnswer - roundId: {}, userUid: {}, answerText: {}", 
            roundId, req.userUid(), req.answerText());
        
        // [VALIDATION 1] Round exists
        QuizRound round = roundRepo.findById(roundId)
                .orElseThrow(() -> new NoSuchElementException("Round not found: " + roundId));
        log.debug("[QUIZ] Found round - sessionId: {}, questionId: {}", round.getSessionId(), round.getQuestion().getId());
        
        // [VALIDATION 2] Round not closed
        if (round.getEndedAt() != null) {
            log.warn("[QUIZ] Round already closed - roundId: {}, endedAt: {}", roundId, round.getEndedAt());
            throw new IllegalStateException("Round already closed");
        }
        
        // [VALIDATION 3] Session ID match (if provided in request)
        if (req.sessionId() != null && !req.sessionId().equals(round.getSessionId())) {
            log.warn("[QUIZ] Session ID mismatch - expected: {}, provided: {}", round.getSessionId(), req.sessionId());
            throw new IllegalArgumentException("Session ID mismatch: round belongs to session " + round.getSessionId() + 
                ", but request specified " + req.sessionId());
        }
        
        // [VALIDATION 4] Duplicate submission check
        boolean existingAnswer = answerRepo.existsByRoundAndUserUid(round, req.userUid());
        if (existingAnswer) {
            log.warn("[QUIZ] Duplicate submission - roundId: {}, userUid: {}", roundId, req.userUid());
            throw new IllegalStateException("Answer already submitted - duplicate");
        }

        GameSession session = gameRepo.findById(round.getSessionId())
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + round.getSessionId()));

        // [VALIDATION 5] Option ID validation (will throw IllegalArgumentException if invalid)
        QuizAnswer answer = new QuizAnswer(round, req.userUid(), req.answerText());

        boolean correct = isCorrect(round.getQuestion(), req.answerText());
        
        // 🔥 응답시간 계산 수정: 클라이언트가 제공한 responseTimeMs 사용 (더 정확)
        long responseTimeMs = req.responseTimeMs() != null ? req.responseTimeMs() : 0L;
        
        if (correct) {
            answer.grade(true, responseTimeMs);
        } else {
            answer.grade(false, 0L);
        }
        answerRepo.save(answer);
        
        // 🔥 [ANSWERS-AFTER] 구조화된 로그 추가
        log.info("[ANSWERS-AFTER] sid={}, rid={}, uid={}, isCorrect={}, rtMs={}, score={}, answerId={}", 
            round.getSessionId(), roundId, req.userUid(), correct, 
            correct ? answer.getResponseTimeMs() : 0L, 
            correct ? 1 : 0, 
            answer.getId());
        
        log.info("[QUIZ] Answer submitted - User: {}, Correct: {}, Round: {}", req.userUid(), correct, roundId);

        // 실제 세션 참여자 수를 가져옴 (온라인 상태인 멤버만 카운팅)
        List<GameSessionMember> activeMembers = memberRepo.findBySessionId(round.getSessionId());
        int totalPlayers = activeMembers.size();
        long answeredPlayers = answerRepo.countDistinctUserUidsByRound(round);
        
        log.info("[QUIZ] Round progress - Answered: {}/{}, Round: {}, Active members: {}", 
                answeredPlayers, totalPlayers, roundId, activeMembers.stream().map(GameSessionMember::getUserUid).toList());
        
        // 🔥 답변 후 afterCommit으로 점수판 업데이트 브로드캐스트 (트랜잭션 커밋 후 실행)
        log.info("[SCOREBOARD-PUB] sid={}, reason=ANSWER", round.getSessionId());
        afterCommit(() -> broadcastScoreboard(round.getSessionId()));
        
        // 라운드 종료 체크 (모든 플레이어가 답변하면 다음 라운드 시작)
        // 최소 1명의 플레이어라도 있어야 진행
        if (totalPlayers > 0 && answeredPlayers >= totalPlayers) {
            log.info("[QUIZ] All players answered ({}/{}) - progressing to next round", answeredPlayers, totalPlayers);
            
            try {
                // ROUND_END 메시지 브로드캐스트 (정답 공개)
                broadcastRoundEnd(round);
                
                // 라운드 종료 처리 - 즉시 종료하여 active round 체크를 우회
                log.info("[ROUND-END] sid={}, rid={}, submitted={}/{}, reason=ALL_SUBMITTED", 
                    round.getSessionId(), roundId, answeredPlayers, totalPlayers);
                round.endRound(); // endedAt을 현재 시간으로 설정
                roundRepo.save(round);
                
                // 다음 라운드 시작 또는 게임 종료 - 종료 조건 개선
                long currentRoundCount = roundRepo.countBySessionId(round.getSessionId());
                log.info("[QUIZ] Round progression check - current rounds: {}, total rounds: {}, current round no: {}", 
                        currentRoundCount, session.getTotalRounds(), round.getRoundNo());
                
                // 현재 라운드 번호가 총 라운드 수와 같거나 크면 게임 종료
                if (session.getTotalRounds() != null && round.getRoundNo() >= session.getTotalRounds()) {
                    log.info("[QUIZ] Game ending - completed round {}/{}", round.getRoundNo(), session.getTotalRounds());
                    
                    // 게임 종료
                    session.finish("Quiz game completed");
                    gameRepo.save(session);
                    
                    // 벌칙 할당
                    assignQuizPenalty(session);
                    
                    // 🔥 [GAME-END] 구조화된 로그 추가
                    log.info("[GAME-END] sid={}, rounds={}, totalPlayers={}, penaltyId={}", 
                        session.getId(), session.getTotalRounds(), totalPlayers, 
                        session.getSelectedPenaltyId());
                    
                    // 🔥 점수판 최종 브로드캐스트 (afterCommit 패턴)
                    log.info("[SCOREBOARD-PUB] sid={}, reason=GAME_END", session.getId());
                    afterCommit(() -> broadcastScoreboard(session.getId()));
                } else {
                    log.info("[QUIZ] Creating next round immediately - all players answered, completed round {}/{}", round.getRoundNo(), session.getTotalRounds());
                    
                    // 🔥 [NEXT-ROUND] 로그 추가
                    log.info("[NEXT-ROUND] sid={}, from={}, to={}, isLast=false", 
                        round.getSessionId(), round.getRoundNo(), round.getRoundNo() + 1);
                    
                    // 🔥 FIX: 트랜잭션적으로 즉시 다음 라운드 생성 - 현재 라운드가 이미 종료되었으므로 active check 통과
                    try {
                        startRoundForSession(round.getSessionId());
                        log.info("[NEXT-ROUND] created=SUCCESS for session: {}", round.getSessionId());
                    } catch (Exception e) {
                        log.error("[NEXT-ROUND] created=FAILED, fallback to scheduling: {}", e.getMessage(), e);
                        // 즉시 실행 실패 시 스케줄링으로 폴백
                        scheduleNextRound(round.getSessionId());
                    }
                }
            } catch (Exception e) {
                log.error("[QUIZ] Error during round progression for session {}: {}", round.getSessionId(), e.getMessage(), e);
                // 에러 발생 시에도 다음 라운드 시도
                try {
                    scheduleNextRound(round.getSessionId());
                } catch (Exception retryError) {
                    log.error("[QUIZ] Failed to schedule next round as fallback for session {}: {}", round.getSessionId(), retryError.getMessage());
                }
            }
        }
        
        // 전원 답변 여부 판단 (위에서 계산한 값 재사용)
        boolean allSubmitted = (totalPlayers > 0 && answeredPlayers >= totalPlayers);
        
        return new AnswerResp(
            correct, 
            correct ? 1 : 0, // 점수 (정답 시 1점)
            0, // 총합 점수 (여기서는 계산하지 않음)
            allSubmitted,
            (int) answeredPlayers,
            totalPlayers,
            false // alreadySubmitted
        );
    }

    private void assignQuizPenalty(GameSession session) {
        // 실제 세션 멤버들을 가져옴
        List<GameSessionMember> members = memberRepo.findBySessionId(session.getId());
        List<String> userUids = members.stream()
                .map(GameSessionMember::getUserUid)
                .toList();

        List<UserScore> scores = new ArrayList<>();
        for (String uid : userUids) {
            Long correctCount = answerRepo.countCorrectAnswersByUser(session.getId(), uid);
            Long totalTime = answerRepo.findTotalCorrectResponseTimeByUser(session.getId(), uid);
            scores.add(new UserScore(
                    uid,
                    correctCount != null ? correctCount : 0L,
                    totalTime != null ? totalTime : 0L
            ));
        }

        scores.sort(Comparator
                .comparing(UserScore::correctAnswers)
                .thenComparing(UserScore::totalTime, Comparator.reverseOrder()));

        String loserUid = scores.get(0).userUid();

        Long penaltyId = session.getSelectedPenaltyId();
        Penalty selectedPenalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new IllegalStateException("Selected penalty not found in DB: " + penaltyId));

        GamePenalty gamePenalty = new GamePenalty(session, loserUid, selectedPenalty);
        gamePenaltyRepository.save(gamePenalty);

        session.finish(selectedPenalty.getDescription());
        gameRepo.save(session); // 세션 상태 저장 누락 수정

        // GameResultsResp 생성
        GameResultsResp gameResults = buildGameResults(session.getId(), scores, loserUid, selectedPenalty);
        
        log.info("[QUIZ] Game completed - sessionId: {}, winner: {}, loser: {}, penalty: {}", 
                session.getId(), gameResults.winner() != null ? gameResults.winner().name() : "None", 
                loserUid, selectedPenalty.getDescription());
        
        // 1) 결과를 별도 SSE 이벤트로 브로드캐스트  
        sseService.broadcastToQuizGame(session.getId(), "game-result", gameResults);

        // 2) 기존 게임 종료 메시지도 유지 (기존 클라이언트 호환성)
        String roundTopic = "/topic/quiz/" + session.getId() + "/round";
        Map<String, Object> gameEndPayload = Map.of(
                "type", "GAME_END",
                "data", Map.of(
                    "finalScoreboard", buildFinalScoreboard(scores),
                    "penalty", Map.of(
                        "loserUid", loserUid,
                        "loserNickname", members.stream()
                                .filter(m -> m.getUserUid().equals(loserUid))
                                .findFirst()
                                .map(GameSessionMember::getNickname)
                                .orElse(loserUid.substring(0, Math.min(8, loserUid.length()))),
                        "description", selectedPenalty.getDescription(),
                        "penaltyText", selectedPenalty.getDescription()
                    ),
                    "gameResults", gameResults // 완전한 게임 결과도 포함
                )
        );
        sseService.broadcastToQuizGame(session.getId(), "game-end", gameEndPayload);
        
        log.info("[QUIZ] SSE messages sent for session: {}", session.getId());
    }

    private GameSession findSession(Long sessionId) {
        return gameRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
    }

    private QuizQuestion findQuestion(Long questionId) {
        return findQuestion(questionId, null);
    }

    private QuizQuestion findQuestion(Long questionId, String category) {
        if (questionId == null) {
            // 랜덤 문제를 가져올 때도 options를 함께 로딩
            Page<QuizQuestion> page = questionRepo.search(null, category, PageRequest.of(0, 100));
            List<QuizQuestion> questions = page.getContent();
            
            if (questions.isEmpty()) {
                String errorMsg = category != null ? 
                    "No quiz questions available for category: " + category : 
                    "No quiz questions available";
                throw new IllegalStateException(errorMsg);
            }
            
            return questions.get(new Random().nextInt(questions.size()));
        }
        // 특정 문제를 가져올 때도 options를 함께 로딩
        return questionRepo.findByIdWithOptions(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
    }

    private boolean isCorrect(QuizQuestion question, String answerText) {
        // Try optionId first if answerText is numeric
        try {
            Long optionId = Long.parseLong(answerText);
            return isCorrectByOptionId(question, optionId);
        } catch (NumberFormatException e) {
            // Fall back to text-based matching
            String normalizedAnswer = normalize(answerText);
            List<QuizQuestionOption> options = optionRepo.findByQuestion(question);

            return options.stream()
                    .filter(QuizQuestionOption::isCorrect)
                    .map(o -> normalize(o.getOptionText()))
                    .anyMatch(normalizedAnswer::equals);
        }
    }
    
    /**
     * Validates that the optionId belongs to the question and returns correctness
     */
    private boolean isCorrectByOptionId(QuizQuestion question, Long optionId) {
        List<QuizQuestionOption> options = optionRepo.findByQuestion(question);
        
        // First validate that optionId belongs to this question
        QuizQuestionOption selectedOption = options.stream()
                .filter(opt -> opt.getOptionId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Option ID " + optionId + " is not valid for this question"));
        
        // Return correctness
        return selectedOption.isCorrect();
    }
    
    private String normalize(String s) {
        if (s == null) return "";
        s = s.trim().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        return Normalizer.normalize(s, Normalizer.Form.NFKC);
    }

    @Transactional
    public synchronized RoundResp startRound(Long sessionId, String category) {
        log.info("[QUIZ] Starting round for sessionId: {}, category: {} (synchronized)", sessionId, category);
        
        GameSession session = findSession(sessionId);
        if (session.getGameType() != GameSession.GameType.QUIZ) {
            throw new IllegalStateException("Not a quiz session");
        }

        // 이미 활성 라운드가 있는지 확인
        List<QuizRound> activeRounds = roundRepo.findBySessionIdOrderByStartsAtDesc(sessionId);
        for (QuizRound existingRound : activeRounds) {
            if (existingRound.getExpiresAt() != null && existingRound.getExpiresAt().isAfter(LocalDateTime.now())) {
                log.info("[QUIZ] Active round already exists for session: {}, roundId: {}", sessionId, existingRound.getRoundId());
                return RoundResp.from(existingRound);
            }
        }

        session.start();
        log.info("[QUIZ] Session started for sessionId: {}", sessionId);

        // Find a random question from the specified category
        Page<QuizQuestion> questionsPage = questionRepo.search(null, category, PageRequest.of(0, 100));
        List<QuizQuestion> questions = questionsPage.getContent();
        log.info("[QUIZ] Found {} questions for category: {}", questions.size(), category);
        
        if (questions.isEmpty()) {
            throw new IllegalStateException("No questions found for category: " + category);
        }
        
        QuizQuestion question = questions.get(new Random().nextInt(questions.size()));
        log.info("[QUIZ] Selected question id: {} for sessionId: {}", question.getId(), sessionId);
        
        // 라운드 번호 계산
        long existingRoundCount = roundRepo.countBySessionId(sessionId);
        int nextRoundNo = (int) existingRoundCount + 1;
        
        QuizRound round = new QuizRound(sessionId, nextRoundNo, question);
        QuizRound savedRound = roundRepo.save(round);
        log.info("[QUIZ] Saved round id: {} (roundNo: {}) for sessionId: {}", savedRound.getRoundId(), nextRoundNo, sessionId);

        // Send WebSocket notification - 통일된 topic 패턴 사용
        String roundTopic = "/topic/quiz/" + sessionId + "/round";
        Map<String, Object> roundPayload = Map.of(
                "type", "ROUND_START",
                "data", Map.of(
                    "roundId", savedRound.getRoundId(),
                    "roundNo", savedRound.getRoundNo(),
                    "question", QuizQuestionResp.from(question)
                )
        );
        log.info("[QUIZ] Broadcasting round start via SSE for session: {}", sessionId);
        sseService.broadcastToQuizGame(sessionId, "round-start", roundPayload);
        
        // 점수판 초기화 브로드캐스트 (afterCommit 패턴)
        afterCommit(() -> broadcastScoreboard(sessionId));
        log.info("[QUIZ] Round start completed for sessionId: {}, roundId: {}", sessionId, savedRound.getRoundId());

        return RoundResp.from(savedRound);
    }
    
    /**
     * 트랜잭션 커밋 후 실행할 작업을 등록합니다.
     */
    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            action.run();
                        } catch (Exception e) {
                            log.error("[AFTER-COMMIT] Failed to execute afterCommit action: {}", e.getMessage(), e);
                        }
                    }
                }
            );
        } else {
            // 트랜잭션이 없는 경우 즉시 실행 (테스트 환경 등)
            log.debug("[AFTER-COMMIT] No active transaction, executing immediately");
            action.run();
        }
    }

    /**
     * 점수판을 브로드캐스트합니다.
     */
    private void broadcastScoreboard(Long sessionId) {
        try {
            List<GameSessionMember> members = memberRepo.findBySessionId(sessionId);
            if (members.isEmpty()) {
                log.info("No members found for session: sessionId={}", sessionId);
                return;
            }
            
            List<Map<String, Object>> scoreboard = members.stream()
                .map(member -> {
                    Long correctCount = answerRepo.countCorrectAnswersByUser(sessionId, member.getUserUid());
                    java.util.Map<String, Object> row = new java.util.HashMap<>();
                    String displayName = member.getNickname() != null ? member.getNickname() : member.getUserUid().substring(0, Math.min(8, member.getUserUid().length()));
                    row.put("userUid", member.getUserUid()); // 프론트엔드 호환성을 위해 userUid 사용
                    row.put("uid", member.getUserUid()); // 기존 호환성 유지
                    row.put("nickname", displayName); // 실제 닉네임 사용
                    row.put("nick", displayName); // 기존 호환성 유지
                    row.put("displayName", displayName); // 결과 페이지 호환성
                    int scoreValue = correctCount != null ? correctCount.intValue() : 0;
                    row.put("score", scoreValue);
                    return row;
                })
                .sorted((a, b) -> Integer.compare((Integer) b.get("score"), (Integer) a.get("score"))) // 점수순 정렬
                .toList();
            
            // 프론트엔드 호환성을 위해 직접 배열로 전송
            sseService.broadcastToQuizGame(sessionId, "scoreboard", scoreboard);
            
            log.debug("[QUIZ] Scoreboard broadcast: sessionId={}, members={}", sessionId, scoreboard.size());
        } catch (Exception e) {
            // 점수판 브로드캐스트 실패는 게임 진행을 막지 않음
            log.error("[QUIZ] Failed to broadcast scoreboard for session {}: {}", sessionId, e.getMessage());
        }
    }
    
    /**
     * 라운드 종료 메시지를 브로드캐스트합니다.
     */
    private void broadcastRoundEnd(QuizRound round) {
        try {
            // 정답 옵션 찾기
            QuizQuestion question = round.getQuestion();
            List<QuizQuestionOption> options = optionRepo.findByQuestion(question);
            
            // 정답 옵션 ID 찾기
            Integer correctOptionId = null;
            for (QuizQuestionOption option : options) {
                if (option.getIsCorrect()) {
                    correctOptionId = option.getOptionId().intValue();
                    break;
                }
            }
            
            // ROUND_END 메시지 브로드캐스트
            String roundTopic = "/topic/quiz/" + round.getSessionId() + "/round";
            Map<String, Object> roundEndMessage = Map.of(
                "type", "ROUND_END",
                "roundId", round.getRoundId(),
                "correctOptionId", correctOptionId != null ? correctOptionId : -1
            );
            sseService.broadcastToQuizGame(round.getSessionId(), "round-end", roundEndMessage);
            
            log.debug("[QUIZ] Round end broadcast: sessionId={}, roundId={}, correctOption={}", 
                round.getSessionId(), round.getRoundId(), correctOptionId);
        } catch (Exception e) {
            log.error("[QUIZ] Failed to broadcast round end for round {}: {}", round.getRoundId(), e.getMessage());
        }
    }
    
    /**
     * 최종 점수판을 생성합니다.
     */
    private List<Map<String, Object>> buildFinalScoreboard(List<UserScore> scores) {
        List<UserScore> sortedScores = scores.stream()
            .sorted(Comparator.comparing(UserScore::correctAnswers).reversed() // 점수 높은 순으로 정렬
                   .thenComparing(UserScore::totalTime)) // 시간 짧은 순으로 보조 정렬
            .toList();
            
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < sortedScores.size(); i++) {
            UserScore score = sortedScores.get(i);
            java.util.Map<String, Object> row = new java.util.HashMap<>();
            row.put("userUid", score.userUid());
            row.put("displayName", score.userUid().substring(0, Math.min(8, score.userUid().length())));
            row.put("nickname", score.userUid().substring(0, Math.min(8, score.userUid().length())));
            row.put("score", (int) score.correctAnswers()); // score 필드 명확히 설정
            row.put("correctAnswers", score.correctAnswers());
            row.put("totalTime", score.totalTime());
            row.put("rank", i + 1); // 순위 추가 (1등부터)
            result.add(row);
        }
        return result;
    }
    
    /**
     * GameResultsResp를 생성합니다 (승자, 순위, 벌칙 정보 포함)
     */
    private GameResultsResp buildGameResults(Long sessionId, List<UserScore> scores, String loserUid, Penalty penalty) {
        // 실제 멤버 정보를 가져와서 닉네임 매핑
        List<GameSessionMember> members = memberRepo.findBySessionId(sessionId);
        Map<String, String> userNicknames = members.stream()
                .collect(java.util.stream.Collectors.toMap(
                        GameSessionMember::getUserUid,
                        m -> m.getNickname() != null ? m.getNickname() : m.getUserUid().substring(0, Math.min(8, m.getUserUid().length()))
                ));
        
        // 점수를 기반으로 순위별 PlayerResult 생성 (1등이 승자)
        List<GameResultsResp.PlayerResult> ranking = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            UserScore score = scores.get(scores.size() - 1 - i); // 역순으로 1등부터
            String displayName = userNicknames.getOrDefault(score.userUid(), score.userUid().substring(0, Math.min(8, score.userUid().length())));
            ranking.add(GameResultsResp.PlayerResult.builder()
                    .uid(score.userUid())
                    .name(displayName)
                    .score((int) score.correctAnswers()) // 정답 개수를 score로 설정
                    .rank(i + 1)
                    .build());
        }
        
        // 승자는 1등 (가장 높은 점수)
        GameResultsResp.PlayerResult winner = ranking.isEmpty() ? null : ranking.get(0);
        
        // 벌칙 정보 생성
        List<GameResultsResp.PlayerResult> penaltyTargets = ranking.stream()
                .filter(p -> p.uid().equals(loserUid))
                .toList();
        
        GameResultsResp.PenaltyResult penaltyResult = GameResultsResp.PenaltyResult.builder()
                .assigned(true)
                .rule("최하위자")
                .targets(penaltyTargets)
                .content(penalty.getDescription())
                .build();
        
        return GameResultsResp.builder()
                .sessionId(sessionId)
                .gameType("QUIZ")
                .winner(winner)
                .ranking(ranking)
                .penalty(penaltyResult)
                .completedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 다음 라운드를 스케줄링합니다. (3초 뒤레이 적용)
     */
    private void scheduleNextRound(Long sessionId) {
        try {
            log.info("[NEXT-ROUND] Scheduling next round via delay for session: {} (1s delay)", sessionId);
            
            // 즉시 다음 라운드 시작 (트랜잭션 컨텍스트 유지)
            try {
                Thread.sleep(1000); // 1초만 대기하여 클라이언트가 결과를 볼 시간 확보
                startRoundForSession(sessionId);
                log.info("[NEXT-ROUND] Scheduled round started successfully for session: {}", sessionId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[NEXT-ROUND] Scheduling interrupted for session: {}", sessionId);
            } catch (Exception e) {
                log.error("[NEXT-ROUND] Failed to start scheduled round for session {}: {}", sessionId, e.getMessage(), e);
                // 재시도 로직 추가
                retryStartNextRound(sessionId);
            }
            
        } catch (Exception e) {
            log.error("[NEXT-ROUND] Failed to schedule next round for session {}: {}", sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 다음 라운드 시작 재시도
     */
    private void retryStartNextRound(Long sessionId) {
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 2초 후 재시도
                log.info("[NEXT-ROUND] Retrying to start next round for session: {}", sessionId);
                startRoundForSession(sessionId);
            } catch (Exception e) {
                log.error("[NEXT-ROUND] Retry failed to start next round for session {}: {}", sessionId, e.getMessage());
            }
        }).start();
    }
    
    /**
     * 라운드 강제 진행 타이머 (30초 후 자동 진행)
     */
    private void scheduleRoundForceProgress(Long roundId, Long sessionId) {
        new Thread(() -> {
            try {
                Thread.sleep(35000); // 35초 후 강제 진행 (클라이언트 30초 + 5초 여유)
                
                // 라운드가 아직 진행 중인지 확인
                QuizRound round = roundRepo.findById(roundId).orElse(null);
                if (round != null && round.getEndedAt() == null) {
                    log.info("[QUIZ] Forcing round progress due to timeout - roundId: {}, sessionId: {}", roundId, sessionId);
                    
                    try {
                        GameSession session = findSession(sessionId);
                        
                        // ROUND_END 메시지 브로드캐스트
                        broadcastRoundEnd(round);
                        
                        // 라운드 종료 처리  
                        long timeoutAnsweredPlayers = answerRepo.countByRoundRoundId(roundId);
                        int timeoutTotalPlayers = memberRepo.findBySessionId(sessionId).size();
                        log.info("[ROUND-END] sid={}, rid={}, submitted={}/{}, reason=TIMEOUT", 
                            sessionId, roundId, timeoutAnsweredPlayers, timeoutTotalPlayers);
                        round.endRound();
                        roundRepo.save(round);
                        
                        // 다음 라운드 시작 또는 게임 종료 - 타이머 강제 진행
                        long currentRoundCount = roundRepo.countBySessionId(sessionId);
                        log.info("[QUIZ] Force progress check - current rounds: {}, total rounds: {}, current round no: {}", 
                                currentRoundCount, session.getTotalRounds(), round.getRoundNo());
                        
                        if (session.getTotalRounds() != null && round.getRoundNo() >= session.getTotalRounds()) {
                            log.info("[QUIZ] Game ending due to force progress - completed round {}/{}", round.getRoundNo(), session.getTotalRounds());
                            session.finish("Quiz game completed due to force progress");
                            gameRepo.save(session);
                            assignQuizPenalty(session);
                        } else {
                            log.info("[QUIZ] Starting next round due to force progress - completed round {}/{}", round.getRoundNo(), session.getTotalRounds());
                            scheduleNextRound(sessionId);
                        }
                        
                    } catch (Exception e) {
                        log.error("[QUIZ] Error during force progress for roundId {}: {}", roundId, e.getMessage(), e);
                    }
                } else {
                    log.debug("[QUIZ] Round already completed, no need for force progress - roundId: {}", roundId);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[QUIZ] Force progress timer interrupted for roundId: {}", roundId);
            } catch (Exception e) {
                log.error("[QUIZ] Error in force progress timer for roundId {}: {}", roundId, e.getMessage());
            }
        }).start();
    }

    /**
     * 세션 기반 라운드 시작 - 세션에 저장된 카테고리만 사용
     */
    @Transactional
    public synchronized RoundResp startRoundForSession(Long sessionId) {
        log.info("[NEXT-ROUND] Starting next round creation for session: {} (synchronized)", sessionId);
        
        GameSession session = findSession(sessionId);
        if (session.getGameType() != GameSession.GameType.QUIZ) {
            log.error("[NEXT-ROUND] FAILED - Not a quiz session: sid={}, gameType={}", sessionId, session.getGameType());
            throw new IllegalStateException("Not a quiz session");
        }

        // 이미 활성 라운드가 있는지 확인 - 종료되지 않았고 만료되지 않은 라운드만 활성으로 간주
        List<QuizRound> activeRounds = roundRepo.findBySessionIdOrderByStartsAtDesc(sessionId);
        for (QuizRound existingRound : activeRounds) {
            boolean notEnded = existingRound.getEndedAt() == null;
            boolean notExpired = existingRound.getExpiresAt() != null && existingRound.getExpiresAt().isAfter(LocalDateTime.now());
            
            if (notEnded && notExpired) {
                log.warn("[NEXT-ROUND] SKIPPED - Active round still running: sid={}, rid={}, expires={}, ended={}", 
                    sessionId, existingRound.getRoundId(), existingRound.getExpiresAt(), existingRound.getEndedAt());
                return RoundResp.from(existingRound);
            } else {
                log.info("[NEXT-ROUND] Previous round can be replaced: sid={}, rid={}, ended={}, expires={}", 
                    sessionId, existingRound.getRoundId(), existingRound.getEndedAt(), existingRound.getExpiresAt());
            }
        }

        session.start();
        gameRepo.save(session);

        // 세션에 저장된 카테고리 사용
        String categoryToUse = session.getCategory();
        log.info("[ROUND] start session={}, category={}", sessionId, categoryToUse);

        // 카테고리에 따라 문제 선택
        Page<QuizQuestion> questionsPage = questionRepo.search(null, categoryToUse, PageRequest.of(0, 100));
        List<QuizQuestion> questions = questionsPage.getContent();
        
        if (questions.isEmpty()) {
            String errorMsg = categoryToUse != null ? 
                "No questions found for category: " + categoryToUse : 
                "No quiz questions available";
            log.error("[ROUND] {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        QuizQuestion question = questions.get(new Random().nextInt(questions.size()));
        log.info("[ROUND] start session={}, category={} questionId={}", sessionId, categoryToUse, question.getId());
        
        // 라운드 번호 계산 (기존 라운드 수 + 1)
        long existingRounds = roundRepo.countBySessionId(sessionId);
        
        // 게임이 이미 완료되었는지 확인
        if (session.getTotalRounds() != null && existingRounds >= session.getTotalRounds()) {
            log.error("[NEXT-ROUND] FAILED - Game already completed: sid={}, existing={}, total={}", 
                sessionId, existingRounds, session.getTotalRounds());
            throw new IllegalStateException("Game has already completed all rounds (" + existingRounds + "/" + session.getTotalRounds() + ")");
        }
        
        int nextRoundNo = (int) existingRounds + 1;
        
        QuizRound round = new QuizRound(sessionId, nextRoundNo, question);
        QuizRound savedRound = roundRepo.save(round);
        
        log.info("[NEXT-ROUND] SUCCESS - Created new round: sid={}, rid={}, roundNo={}/{}, questionId={}", 
                sessionId, savedRound.getRoundId(), nextRoundNo, 
                session.getTotalRounds(), question.getId());
        
        // 30초 후 강제로 다음 라운드 진행하는 타이머 시작
        scheduleRoundForceProgress(savedRound.getRoundId(), sessionId);

        // Send WebSocket notification - 지시사항에 따른 ROUND_START 메시지 형식
        String roundTopic = "/topic/quiz/" + sessionId + "/round";
        
        // 옵션 정보 포함한 질문 데이터 구성
        List<QuizQuestionOption> options = optionRepo.findByQuestion(question);
        List<Map<String, Object>> optionList = options.stream()
            .map(option -> {
                Map<String, Object> optionData = new HashMap<>();
                optionData.put("id", option.getOptionId());
                optionData.put("text", option.getOptionText());
                return optionData;
            })
            .toList();
        
        Map<String, Object> questionData = Map.of(
            "id", question.getId(),
            "text", question.getQuestionText(),
            "options", optionList
        );
        
        Map<String, Object> roundPayload = Map.of(
            "type", "ROUND_START",
            "data", Map.of(
                "roundId", savedRound.getRoundId(),
                "roundNo", savedRound.getRoundNo(),
                "question", QuizQuestionResp.from(question)
            )
        );
        sseService.broadcastToQuizGame(sessionId, "round-start", roundPayload);
        
        // 점수판 초기화 브로드캐스트 (afterCommit 패턴)
        afterCommit(() -> broadcastScoreboard(sessionId));

        return RoundResp.from(savedRound);
    }

    /**
     * 세션 존재 여부 확인 (방탄 로직)
     */
    @Transactional(readOnly = true)
    public boolean existsSession(Long sessionId) {
        try {
            if (sessionId == null) {
                return false;
            }
            return gameRepo.existsById(sessionId);
        } catch (Exception e) {
            log.warn("Failed to check session existence: sessionId={}", sessionId, e);
            return false; // 오류 발생 시 존재하지 않는 것으로 처리
        }
    }

    /**
     * 스코어보드 조회 (500 에러 절대 금지)
     */
    @Transactional(readOnly = true)
    public List<ScoreboardItem> getScoreboard(Long sessionId) {
        try {
            if (sessionId == null) {
                log.warn("getScoreboard called with null sessionId");
                return List.of(); // 빈 리스트 반환
            }

            // 세션 존재 확인
            if (!gameRepo.existsById(sessionId)) {
                log.warn("Session not found: sessionId={}", sessionId);
                return List.of(); // 빈 리스트 반환 (404는 컨트롤러에서 처리)
            }

            // 세션 멤버 조회 (LEFT JOIN 방식으로 방탄 처리)
            List<GameSessionMember> members = memberRepo.findBySessionId(sessionId);
            if (members == null || members.isEmpty()) {
                log.info("No members found for session: sessionId={}", sessionId);
                return List.of(); // 빈 리스트 반환
            }

            List<ScoreboardItem> scoreboard = new ArrayList<>();
            int rank = 1;

            for (GameSessionMember member : members) {
                try {
                    String userUid = member.getUserUid();
                    if (userUid == null || userUid.trim().isEmpty()) {
                        continue; // 잘못된 멤버는 건너뛰기
                    }

                    // 정답 수 조회 (null safe)
                    Long correctCount = answerRepo.countCorrectAnswersByUser(sessionId, userUid);
                    int score = correctCount != null ? correctCount.intValue() : 0;

                    // 총 답변 수 조회 (null safe)
                    Long totalAnswered = answerRepo.countAnswersByUser(sessionId, userUid);
                    int totalAnsweredInt = totalAnswered != null ? totalAnswered.intValue() : 0;

                    scoreboard.add(new ScoreboardItem(
                            userUid,
                            score,
                            score, // correctCount와 동일
                            totalAnsweredInt,
                            rank++
                    ));

                } catch (Exception e) {
                    log.warn("Failed to process member in scoreboard: sessionId={}, member={}", 
                            sessionId, member.getUserUid(), e);
                    // 개별 멤버 처리 실패는 전체 결과에 영향주지 않음
                }
            }

            // 점수 기준 정렬 (높은 점수부터)
            scoreboard.sort((a, b) -> Integer.compare(b.score(), a.score()));

            // 순위 재계산
            List<ScoreboardItem> rankedScoreboard = new ArrayList<>();
            for (int i = 0; i < scoreboard.size(); i++) {
                ScoreboardItem item = scoreboard.get(i);
                rankedScoreboard.add(new ScoreboardItem(
                        item.userUid(),
                        item.score(),
                        item.correctCount(),
                        item.totalAnswered(),
                        i + 1 // 1부터 시작하는 순위
                ));
            }

            log.info("Generated scoreboard for sessionId={}, members={}", sessionId, rankedScoreboard.size());
            return rankedScoreboard;

        } catch (Exception e) {
            log.error("Critical error in getScoreboard: sessionId={}", sessionId, e);
            return List.of(); // 모든 예외 상황에서 빈 리스트 반환 (500 절대 금지)
        }
    }
    
    /**
     * 게임 결과 조회 (승자, 순위, 벌칙 정보)
     */
    @Transactional(readOnly = true)
    public GameResultsResp getGameResults(Long sessionId) {
        log.info("Getting game results: sessionId={}", sessionId);
        
        GameSession session = findSession(sessionId);
        if (session.getStatus() != GameSession.Status.FINISHED) {
            throw new IllegalStateException("Game not finished yet: " + sessionId);
        }
        
        // 세션 멤버들과 점수 계산 (assignQuizPenalty와 동일한 로직)
        List<GameSessionMember> members = memberRepo.findBySessionId(sessionId);
        List<String> userUids = members.stream()
                .map(GameSessionMember::getUserUid)
                .toList();

        List<UserScore> scores = new ArrayList<>();
        for (String uid : userUids) {
            Long correctCount = answerRepo.countCorrectAnswersByUser(sessionId, uid);
            Long totalTime = answerRepo.findTotalCorrectResponseTimeByUser(sessionId, uid);
            scores.add(new UserScore(
                    uid,
                    correctCount != null ? correctCount : 0L,
                    totalTime != null ? totalTime : 0L
            ));
        }

        scores.sort(Comparator
                .comparing(UserScore::correctAnswers)
                .thenComparing(UserScore::totalTime, Comparator.reverseOrder()));

        // 벌칙 정보 조회
        GamePenalty gamePenalty = gamePenaltyRepository.findByGameSessionId(sessionId)
                .orElseThrow(() -> new IllegalStateException("Game penalty not found: " + sessionId));
        
        Penalty penalty = penaltyRepository.findById(gamePenalty.getPenalty().getId())
                .orElseThrow(() -> new IllegalStateException("Penalty not found: " + gamePenalty.getPenalty().getId()));
        
        return buildGameResults(sessionId, scores, gamePenalty.getUserUid(), penalty);
    }

    /**
     * 현재 활성 라운드 조회 (500 에러 절대 금지)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentRound(Long sessionId) {
        try {
            if (sessionId == null) {
                log.warn("getCurrentRound called with null sessionId");
                return null;
            }

            // 세션에서 가장 최근의 진행 중인 라운드 조회
            List<QuizRound> activeRounds = roundRepo.findBySessionIdOrderByStartsAtDesc(sessionId);
            
            if (activeRounds == null || activeRounds.isEmpty()) {
                log.info("No rounds found for sessionId={}, will not auto-start in read-only context", sessionId);
                return null;
            }

            // 가장 최근 라운드 중에서 아직 만료되지 않은 것 찾기
            QuizRound currentRound = null;
            for (QuizRound round : activeRounds) {
                if (round.getExpiresAt() != null && round.getExpiresAt().isAfter(java.time.LocalDateTime.now())) {
                    currentRound = round;
                    break;
                }
            }

            if (currentRound == null) {
                log.debug("No active (non-expired) rounds found for sessionId={}", sessionId);
                return null;
            }

            // 세션 조회하여 카테고리 정보 가져오기
            GameSession session = findSession(sessionId);
            
            // 🔥 게임이 완료된 경우 라운드 반환 안함 (204 반환을 위해)
            if (session.getStatus() == GameSession.Status.FINISHED) {
                log.debug("Game is FINISHED for sessionId={}, returning null to trigger 204", sessionId);
                return null;
            }
            
            // 라운드 데이터 구성
            Map<String, Object> roundData = new HashMap<>();
            roundData.put("roundId", currentRound.getRoundId());
            roundData.put("roundNo", currentRound.getRoundNo());
            roundData.put("sessionId", currentRound.getSessionId());
            roundData.put("category", session.getCategory()); // 세션의 카테고리 사용 (단일 소스)
            roundData.put("expiresAt", currentRound.getExpiresAt().atZone(java.time.ZoneOffset.UTC).toInstant());
            
            // 🔥 시간 동기화를 위한 서버 시간 추가 (ms 단위 통일)
            roundData.put("expiresAtMs", currentRound.getExpiresAt().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli());
            roundData.put("serverTimeMs", java.time.Instant.now().toEpochMilli());

            // 질문 정보 추가
            QuizQuestion question = currentRound.getQuestion();
            if (question != null) {
                Map<String, Object> questionData = new HashMap<>();
                questionData.put("id", question.getId());
                questionData.put("text", question.getQuestionText());
                questionData.put("category", question.getCategory());

                // 옵션 조회 - ID와 텍스트 포함한 객체 배열로 반환
                List<QuizQuestionOption> options = optionRepo.findByQuestion(question);
                List<Map<String, Object>> optionList = options.stream()
                    .map(option -> {
                        Map<String, Object> optionData = new HashMap<>();
                        optionData.put("id", option.getOptionId());
                        optionData.put("text", option.getOptionText());
                        return optionData;
                    })
                    .toList();
                questionData.put("options", optionList);

                roundData.put("question", questionData);
            }

            log.debug("Current round found: sessionId={}, roundId={}", sessionId, currentRound.getRoundId());
            return roundData;

        } catch (Exception e) {
            log.error("Critical error in getCurrentRound: sessionId={}", sessionId, e);
            return null; // 모든 예외 상황에서 null 반환 (500 절대 금지)
        }
    }

    /**
     * 라운드 목록 조회 (status 필터링 지원, 500 에러 절대 금지)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRounds(Long sessionId, String status, int limit) {
        try {
            if (sessionId == null) {
                log.warn("getRounds called with null sessionId");
                return List.of();
            }

            List<QuizRound> rounds;
            
            // status 필터링
            if ("ACTIVE".equalsIgnoreCase(status)) {
                // 활성 라운드만 조회 (아직 만료되지 않은 라운드)
                rounds = roundRepo.findBySessionIdOrderByStartsAtDesc(sessionId);
                rounds = rounds.stream()
                    .filter(round -> round.getExpiresAt() != null && 
                                   round.getExpiresAt().isAfter(java.time.LocalDateTime.now()))
                    .limit(Math.min(limit, 50)) // 최대 50개 제한
                    .toList();
            } else {
                // 모든 라운드 조회
                rounds = roundRepo.findBySessionIdOrderByStartsAtDesc(sessionId)
                    .stream()
                    .limit(Math.min(limit, 50)) // 최대 50개 제한
                    .toList();
            }

            List<Map<String, Object>> roundsData = new ArrayList<>();
            for (QuizRound round : rounds) {
                try {
                    Map<String, Object> roundData = new HashMap<>();
                    roundData.put("roundId", round.getRoundId());
                    roundData.put("roundNo", round.getRoundNo());
                    roundData.put("sessionId", round.getSessionId());
                    
                    if (round.getStartsAt() != null) {
                        roundData.put("startsAt", round.getStartsAt().atZone(java.time.ZoneOffset.UTC).toInstant());
                    }
                    if (round.getExpiresAt() != null) {
                        roundData.put("expiresAt", round.getExpiresAt().atZone(java.time.ZoneOffset.UTC).toInstant());
                    }

                    // 질문 정보 추가
                    QuizQuestion question = round.getQuestion();
                    if (question != null) {
                        Map<String, Object> questionData = new HashMap<>();
                        questionData.put("id", question.getId());
                        questionData.put("text", question.getQuestionText());
                        questionData.put("category", question.getCategory());

                        // 옵션 조회
                        List<QuizQuestionOption> options = optionRepo.findByQuestion(question);
                        List<String> optionTexts = options.stream()
                            .map(QuizQuestionOption::getOptionText)
                            .toList();
                        questionData.put("options", optionTexts);

                        roundData.put("question", questionData);
                    }

                    roundsData.add(roundData);
                } catch (Exception e) {
                    log.warn("Failed to process round in getRounds: sessionId={}, roundId={}", 
                            sessionId, round.getRoundId(), e);
                    // 개별 라운드 처리 실패는 전체 결과에 영향주지 않음
                }
            }

            log.debug("Rounds retrieved: sessionId={}, status={}, count={}", sessionId, status, roundsData.size());
            return roundsData;

        } catch (Exception e) {
            log.error("Critical error in getRounds: sessionId={}, status={}", sessionId, status, e);
            return List.of(); // 모든 예외 상황에서 빈 리스트 반환 (500 절대 금지)
        }
    }
    
    /**
     * 30초 후 라운드 자동 종료 스케줄링
     */
    private void scheduleRoundTimeout(Long roundId, Long sessionId) {
        new Thread(() -> {
            try {
                Thread.sleep(30000); // 30초 대기
                
                // 라운드가 아직 진행 중인지 확인
                QuizRound round = roundRepo.findById(roundId).orElse(null);
                if (round != null && round.getEndedAt() == null) {
                    log.info("[QUIZ] Round timeout - auto ending round: {}", roundId);
                    
                    // 라운드 종료 처리
                    handleRoundTimeout(round);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[QUIZ] Round timeout scheduling interrupted: {}", roundId);
            } catch (Exception e) {
                log.error("[QUIZ] Error in round timeout handling: {}", roundId, e);
            }
        }).start();
    }
    
    /**
     * 라운드 시간 초과 처리
     */
    @Transactional
    private void handleRoundTimeout(QuizRound round) {
        try {
            GameSession session = findSession(round.getSessionId());
            
            // ROUND_END 메시지 브로드캐스트
            broadcastRoundEnd(round);
            
            // 라운드 종료 처리
            round.endRound();
            roundRepo.save(round);
            
            // 다음 라운드 시작 또는 게임 종료
            long currentRoundCount = roundRepo.countBySessionId(round.getSessionId());
            if (session.getTotalRounds() != null && currentRoundCount >= session.getTotalRounds()) {
                log.info("[QUIZ] Game ending due to timeout - total rounds completed: {}", currentRoundCount);
                assignQuizPenalty(session);
            } else {
                log.info("[QUIZ] Starting next round due to timeout - current: {}/{}", currentRoundCount, session.getTotalRounds());
                scheduleNextRound(round.getSessionId());
            }
            
        } catch (Exception e) {
            log.error("[QUIZ] Error handling round timeout: {}", round.getRoundId(), e);
        }
    }

    /**
     * 라운드 ID로 문제 정보 조회 (500 에러 절대 금지)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getQuestionByRoundId(Long roundId) {
        try {
            if (roundId == null) {
                log.warn("getQuestionByRoundId called with null roundId");
                return null;
            }

            Optional<QuizRound> roundOpt = roundRepo.findById(roundId);
            if (roundOpt.isEmpty()) {
                log.warn("Round not found: roundId={}", roundId);
                return null;
            }

            QuizRound round = roundOpt.get();
            QuizQuestion question = round.getQuestion();
            if (question == null) {
                log.warn("Question not found: roundId={}", roundId);
                return null;
            }

            Map<String, Object> questionData = new HashMap<>();
            questionData.put("questionId", question.getId());
            questionData.put("sentence", question.getQuestionText());
            
            // 옵션을 리스트로 구성 (options 관계에서 가져오기)
            List<String> options = question.getOptions().stream()
                .map(option -> option.getOptionText())
                .toList();
            questionData.put("options", options);
            
            questionData.put("category", question.getCategory());
            questionData.put("timeLimitSec", 30); // 기본 제한시간
            questionData.put("roundId", roundId);
            questionData.put("roundNo", round.getRoundNo());
            
            // Status 계산 (endedAt 기준)
            String status = (round.getEndedAt() != null) ? "FINISHED" : "ACTIVE";
            questionData.put("status", status);

            log.info("Question found for roundId={}: questionId={}", roundId, question.getId());
            return questionData;

        } catch (Exception e) {
            log.error("Critical error in getQuestionByRoundId: roundId={}", roundId, e);
            return null; // 500 절대 금지
        }
    }
    
    /**
     * 라운드 ID로 세션 ID 조회 (레거시 경로 지원용)
     */
    @Transactional(readOnly = true)
    public Long getSessionIdByRoundId(Long roundId) {
        QuizRound round = roundRepo.findById(roundId)
            .orElseThrow(() -> new NoSuchElementException("Round not found: " + roundId));
        return round.getSessionId();
    }
    
    /**
     * 🚀 멱등성 지원 답변 제출 (410 Gone + 중복 처리)
     */
    @Transactional
    public AnswerResp submitAnswerIdempotent(Long sessionId, Long roundId, String userUid, Long optionId, Long responseTimeMs) {
        log.info("[ANSWERS-BEFORE] sid={}, rid={}, uid={}, optionId={}", 
            sessionId, roundId, userUid, optionId);
        
        // 1. 라운드 존재 및 활성 상태 확인 → 404 Not Found  
        QuizRound round = roundRepo.findById(roundId)
            .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));
            
        // 2. 세션 ID 검증 → 404 Not Found
        if (!round.getSessionId().equals(sessionId)) {
            throw new IllegalArgumentException("Round belongs to different session");
        }
        
        // 3. 옵션이 이 라운드에 속하는지 확인 → 422 Unprocessable Entity
        if (!optionRepo.existsByRoundIdAndOptionId(roundId, optionId)) {
            throw new su.kdt.minigame.exception.InvalidOptionException(roundId, optionId);
        }
        
        // 4. 라운드가 이미 종료되었는지 확인 → 410 Gone
        if (round.getEndedAt() != null) {
            log.info("[QUIZ-IDEM] Round already ended: roundId={}, endedAt={}", roundId, round.getEndedAt());
            
            List<GameSessionMember> members = memberRepo.findBySessionId(sessionId);
            int totalPlayers = members.size();
            long answeredPlayers = answerRepo.countDistinctUserUidsByRound(round);
            
            GameSession session = gameRepo.findById(sessionId).orElse(null);
            String phase = (session != null && session.getStatus() == GameSession.Status.FINISHED) 
                ? "FINISHED" : "WAITING_NEXT";
            
            throw new su.kdt.minigame.exception.RoundGoneException(
                sessionId, roundId, phase, (int) answeredPlayers, totalPlayers);
        }
        
        // 5. 새로운 답변 저장 및 처리 (완전 멱등성 보장)
        QuizQuestionOption option = optionRepo.findById(optionId)
            .orElseThrow(() -> new su.kdt.minigame.exception.InvalidOptionException(roundId, optionId));
            
        boolean isCorrect = option.getIsCorrect();
        int score = isCorrect ? 1 : 0;
        
        try {
            // 중복 제출 체크 후 저장 (race condition 방지)
            if (answerRepo.existsByRoundAndUserUid(round, userUid)) {
                log.info("[QUIZ-IDEM] Answer already submitted: roundId={}, userUid={}", roundId, userUid);
                
                // 현재 상태 정보 반환
                List<GameSessionMember> members = memberRepo.findBySessionId(sessionId);
                int totalPlayers = members.size();
                long answeredPlayers = answerRepo.countDistinctUserUidsByRound(round);
                
                return AnswerResp.createAlreadySubmitted()
                    .withSubmittedCount((int) answeredPlayers)
                    .withExpectedParticipants(totalPlayers);
            }
            
            // 정답 텍스트를 옵션에서 가져와서 설정 + 응답시간 계산
            String answerText = option.getOptionText();
            QuizAnswer answer = new QuizAnswer(round, userUid, optionId.intValue()); // choiceIndex 사용
            
            // 🔥 응답시간: 클라이언트가 제공한 responseTimeMs 사용 (더 정확)
            long clientResponseTimeMs = responseTimeMs != null ? responseTimeMs : 0L;
            
            // 채점 (응답시간 포함)
            answer.grade(isCorrect, clientResponseTimeMs);
            answerRepo.save(answer);
            
            log.info("[ANSWERS-AFTER] sid={}, rid={}, uid={}, isCorrect={}, rtMs={}, answerId={}",
                sessionId, roundId, userUid, isCorrect, clientResponseTimeMs, answer.getId());
            
            log.info("[QUIZ-IDEM] Answer saved successfully: roundId={}, userUid={}, isCorrect={}, responseTimeMs={}", 
                roundId, userUid, isCorrect, clientResponseTimeMs);
                
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 유니크 제약 충돌 - 이미 제출된 상태 (멱등 처리)
            log.info("[QUIZ-IDEM] Data integrity violation (already submitted): roundId={}, userUid={}", 
                roundId, userUid);
            
            List<GameSessionMember> members = memberRepo.findBySessionId(sessionId);
            int totalPlayers = members.size();
            long answeredPlayers = answerRepo.countDistinctUserUidsByRound(round);
            
            return AnswerResp.createAlreadySubmitted()
                .withSubmittedCount((int) answeredPlayers)
                .withExpectedParticipants(totalPlayers);
        }
        
        // 7. 제출 상태 확인 및 라운드 완료 처리
        List<GameSessionMember> members = memberRepo.findBySessionId(sessionId);
        int totalPlayers = members.size();
        long answeredPlayers = answerRepo.countDistinctUserUidsByRound(round);
        boolean allSubmitted = (totalPlayers > 0 && answeredPlayers >= totalPlayers);
        
        if (allSubmitted) {
            round.endRound();
            roundRepo.save(round);
            
            // Check if this was the last round
            GameSession session = gameRepo.findById(sessionId).orElse(null);
            if (session != null) {
                List<QuizRound> allRounds = roundRepo.findBySessionId(sessionId);
                if (allRounds.size() >= session.getTotalRounds()) {
                    // Game finished
                    session.finish("Quiz game completed");
                    gameRepo.save(session);
                    assignQuizPenalty(session);
                } else {
                    // Create next round (this could be done asynchronously)
                    try {
                        scheduleNextRound(sessionId);
                    } catch (Exception e) {
                        log.warn("[QUIZ-IDEM] Failed to create next round: {}", e.getMessage());
                    }
                }
            }
        }
        
        return AnswerResp.ok(isCorrect, score, 0, allSubmitted, (int) answeredPlayers, totalPlayers);
    }
}