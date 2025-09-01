package su.kdt.minigame.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.dto.request.CreateRoundReq;
import su.kdt.minigame.dto.request.SubmitAnswerReq;

import su.kdt.minigame.dto.response.AnswerResp;
import su.kdt.minigame.dto.response.GameResultsResp;
import su.kdt.minigame.dto.response.QuizQuestionResp;
import su.kdt.minigame.dto.response.RoundResp;
import su.kdt.minigame.dto.response.ScoreboardItem;

import su.kdt.minigame.domain.GameSession;
import su.kdt.minigame.exception.RoundGoneException;
import su.kdt.minigame.repository.GameRepo;
import su.kdt.minigame.service.QuizService;
import su.kdt.minigame.support.UidResolverFilter;


import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/mini-games")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final GameRepo gameRepo;


    /**
     * 🔥 디버그: 컨트롤러 생존 확인
     */
    @GetMapping("/debug/controller-alive")
    public ResponseEntity<Map<String, Object>> debugControllerAlive() {
        return ResponseEntity.ok(Map.of(
            "message", "QuizController is alive!",
            "timestamp", java.time.Instant.now(),
            "mappings", List.of(
                "POST /sessions/{sessionId}/rounds/{roundId}/answers",
                "POST /rounds/{roundId}/answers"
            )
        ));
    }

    /**
     * 🚀 통합 답변 제출 엔드포인트 (멱등성 + 410 Gone 처리)
     * 두 경로 모두 지원:
     * - POST /api/mini-games/sessions/{sessionId}/rounds/{roundId}/answers (신규)
     * - POST /api/mini-games/rounds/{roundId}/answers (레거시)
     */
    @PostMapping({"/sessions/{sessionId}/rounds/{roundId}/answers", 
                  "/rounds/{roundId}/answers"})
    public ResponseEntity<?> submitAnswerUnified(
            @PathVariable(required = false) Long sessionId,
            @PathVariable Long roundId, 
            @RequestBody SubmitAnswerReq request,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-USER-UID", required = false) String headerUid) {
        log.info("[QUIZ-UNIFIED] 🚀 submitAnswerUnified called: sessionId={}, roundId={}, request={}", 
            sessionId, roundId, request);
        try {
            // 우선순위: Header > Filter > Default
            String userUid = headerUid;
            if (userUid == null || userUid.isBlank()) {
                userUid = (String) httpRequest.getAttribute(UidResolverFilter.ATTR_UID);
            }
            
            if (userUid == null || userUid.isBlank()) {
                log.warn("[ANS] Missing userUid: sessionId={}, roundId={}", sessionId, roundId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "MISSING_USER_UID", "message", "사용자 식별자가 필요합니다."));
            }
            
            // 레거시 경로인 경우 sessionId를 round에서 추출
            if (sessionId == null) {
                sessionId = quizService.getSessionIdByRoundId(roundId);
                log.info("[ANS] Legacy path - extracted sessionId={} from roundId={}", sessionId, roundId);
            }
            
            log.info("[ANS] Submit answer: sessionId={}, roundId={}, userUid={}, optionId={}", 
                sessionId, roundId, userUid, request.optionId());
            
            // 🔥 새로운 멱등성 지원 서비스 호출 (responseTimeMs 포함)
            AnswerResp response = quizService.submitAnswerIdempotent(sessionId, roundId, userUid, request.optionId(), request.responseTimeMs());
            return ResponseEntity.ok(response);
            
        } catch (su.kdt.minigame.exception.RoundGoneException e) {
            // 🚀 410 Gone - 라운드가 이미 닫힌 경우 (정상 상태)
            log.info("[ANS] Round closed: sessionId={}, roundId={}, phase={}", 
                e.getSessionId(), e.getRoundId(), e.getPhase());
            
            Map<String, Object> errorBody = Map.of(
                "code", "ROUND_CLOSED",
                "message", "라운드가 이미 종료되었습니다",
                "phase", e.getPhase(),
                "submittedCount", e.getSubmittedCount(),
                "expectedParticipants", e.getExpectedParticipants()
            );
            return ResponseEntity.status(HttpStatus.GONE).body(errorBody);
            
        } catch (su.kdt.minigame.exception.InvalidOptionException e) {
            log.warn("Invalid option: sessionId={}, roundId={}, optionId={}", sessionId, roundId, e.getOptionId());
            return ResponseEntity.status(422)
                .body(Map.of("code", "INVALID_OPTION_FOR_ROUND", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: sessionId={}, roundId={} - {}", sessionId, roundId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "INVALID_REQUEST", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error submitting answer: sessionId={}, roundId={}", sessionId, roundId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", "INTERNAL_ERROR", "message", "답변 처리 중 오류가 발생했습니다"));
        }
    }


    /**
     * 페이징 처리된 퀴즈 질문 목록을 조회합니다.
     */
    @GetMapping("/questions")
    public ResponseEntity<Page<QuizQuestionResp>> getQuestions(
            @RequestParam(required = false) Long placeId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        try {
            log.info("GET /questions - placeId={}, category={}, page={}, size={}", placeId, category, page, size);
            Pageable pageable = PageRequest.of(page, size);
            Page<QuizQuestionResp> questions = quizService.getQuestions(placeId, category, pageable);
            log.info("Questions response - total={}, content size={}, category={}", 
                questions.getTotalElements(), questions.getContent().size(), category);
            
            // 빈 결과에 대한 명확한 응답
            if (questions.isEmpty() && category != null && !category.trim().isEmpty()) {
                log.warn("No questions found for category: {}", category);
                // Still return 200 with empty page, but log the issue
            }
            
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            log.error("Failed to fetch questions - placeId={}, category={}, error={}", placeId, category, e.getMessage(), e);
            throw new IllegalArgumentException("퀴즈 문제 조회에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 세션 기반 단일 문제 조회 API (에러 절대 금지)
     */
    @GetMapping("/sessions/{sessionId}/question")
    public ResponseEntity<?> getOneQuestionBySession(
            @PathVariable Long sessionId,
            @RequestParam(required = false) String category) {
        try {
            log.info("GET /sessions/{}/question - category={}", sessionId, category);
            QuizQuestionResp question = quizService.getSessionQuestion(sessionId, category);
            if (question == null) {
                log.warn("No questions found for sessionId={}, category={}", sessionId, category);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "NO_QUESTION", "message", "해당 세션에 제공할 문제가 없습니다."));
            }
            log.info("Session question response - questionId={}, category={}", question.questionId(), category);
            return ResponseEntity.ok(question);
        } catch (IllegalArgumentException e) {
            log.warn("Bad request for sessionId={}, category={} - {}", sessionId, category, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "BAD_REQUEST", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("GET /sessions/{}/question failed", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", "INTERNAL_SERVER_ERROR", "message", "서버 내부 오류가 발생했습니다."));
        }
    }

    /**
     * 새로운 라운드 시작 API (서버 주도) - 세션에 저장된 카테고리만 사용
     */
    @PostMapping("/sessions/{sessionId}/rounds")
    public ResponseEntity<?> startRound(
        @PathVariable Long sessionId,
        @RequestBody(required = false) CreateRoundReq req
    ) {
        try {
            log.info("[ROUND] start session={}", sessionId);
            
            // 요청 바디의 카테고리는 무시하고 세션에 저장된 카테고리만 사용
            RoundResp response = quizService.startRoundForSession(sessionId);
            return ResponseEntity.ok(response);
        } catch (java.util.NoSuchElementException e) {
            log.warn("Session not found: sessionId={} - {}", sessionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "SESSION_NOT_FOUND", "message", "세션을 찾을 수 없습니다."));
        } catch (IllegalStateException e) {
            // 카테고리 문제 없음 오류 처리
            if (e.getMessage() != null && e.getMessage().contains("No questions")) {
                log.warn("No questions available for sessionId={} - {}", sessionId, e.getMessage());
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("code", "NO_QUESTIONS_FOR_CATEGORY", "message", "선택한 카테고리에 등록된 문제가 없습니다."));
            }
            log.warn("Cannot start round for sessionId={} - {}", sessionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", "INVALID_SESSION_STATE", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("startRound failed: sessionId={}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", "INTERNAL_SERVER_ERROR", "message", "일시 오류, 다시 시도해 주세요."));
        }
    }

    /**
     * 현재 활성 라운드 조회 API (500 에러 절대 금지)
     */
    @GetMapping("/sessions/{sessionId}/current-round")
    public ResponseEntity<?> getCurrentRound(@PathVariable Long sessionId) {
        try {
            log.info("GET /sessions/{}/current-round", sessionId);
            
            // 세션 존재 여부 확인
            if (!quizService.existsSession(sessionId)) {
                log.warn("Session not found: sessionId={}", sessionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "SESSION_NOT_FOUND", "message", "세션을 찾을 수 없습니다."));
            }
            
            // 현재 활성 라운드 조회
            try {
                Map<String, Object> currentRound = quizService.getCurrentRound(sessionId);
                if (currentRound != null) {
                    log.info("Current round found: sessionId={}, roundId={}", sessionId, currentRound.get("roundId"));
                    return ResponseEntity.ok(currentRound);
                } else {
                    log.info("No active round found: sessionId={}", sessionId);
                    
                    // 🔥 FE 자동 전환을 위한 게임 상태 헤더 추가
                    GameSession session = gameRepo.findById(sessionId).orElse(null);
                    String phase = "WAITING_NEXT"; // 기본값: 다음 라운드 대기
                    
                    if (session != null && session.getStatus() == GameSession.Status.FINISHED) {
                        phase = "FINISHED"; // 게임 완료
                        log.info("Game finished for session: {}", sessionId);
                    }
                    
                    return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .header("x-round-phase", phase)
                        .build();
                }
            } catch (Exception e) {
                log.warn("Error retrieving current round: sessionId={}", sessionId, e);
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .header("x-round-phase", "WAITING_NEXT")
                    .build();
            }
            
        } catch (Exception e) {
            log.error("Critical error in getCurrentRound: sessionId={}", sessionId, e);
            // 500 에러 절대 금지 - 204 No Content로 응답
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
    }

    /**
     * 라운드 목록 조회 API (status 필터링 지원, 500 에러 절대 금지)
     */
    @GetMapping("/sessions/{sessionId}/rounds")
    public ResponseEntity<?> getRounds(
            @PathVariable Long sessionId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            log.info("GET /sessions/{}/rounds - status={}, limit={}", sessionId, status, limit);
            
            // 세션 존재 여부 확인
            if (!quizService.existsSession(sessionId)) {
                log.warn("Session not found: sessionId={}", sessionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "SESSION_NOT_FOUND", "message", "세션을 찾을 수 없습니다."));
            }
            
            // 라운드 목록 조회
            try {
                List<Map<String, Object>> rounds = quizService.getRounds(sessionId, status, limit);
                log.info("Rounds found: sessionId={}, count={}", sessionId, rounds.size());
                return ResponseEntity.ok(rounds);
            } catch (Exception e) {
                log.warn("Error retrieving rounds: sessionId={}, status={}", sessionId, status, e);
                return ResponseEntity.ok(List.of()); // 빈 리스트 반환
            }
            
        } catch (Exception e) {
            log.error("Critical error in getRounds: sessionId={}", sessionId, e);
            // 500 에러 절대 금지 - 빈 리스트로 응답
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 스코어보드 조회 API (500 에러 절대 금지)
     */
    @GetMapping("/sessions/{sessionId}/scoreboard")
    public ResponseEntity<List<ScoreboardItem>> getScoreboard(@PathVariable Long sessionId) {
        try {
            log.info("GET /sessions/{}/scoreboard", sessionId);
            
            // 세션 존재 여부 확인
            if (!quizService.existsSession(sessionId)) {
                log.warn("Session not found: sessionId={}", sessionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            // 스코어보드 조회
            List<ScoreboardItem> scoreboard = quizService.getScoreboard(sessionId);
            
            // null 방어 (서비스에서 항상 non-null을 보장하지만 추가 방어)
            if (scoreboard == null) {
                log.warn("Service returned null scoreboard: sessionId={}", sessionId);
                scoreboard = List.of();
            }
            
            log.info("Scoreboard response: sessionId={}, items={}", sessionId, scoreboard.size());
            return ResponseEntity.ok(scoreboard);
            
        } catch (Exception e) {
            log.error("Critical error in getScoreboard: sessionId={}", sessionId, e);
            // 500 에러 절대 금지 - 빈 리스트로 응답
            return ResponseEntity.ok(List.of());
        }
    }
    
    /**
     * 게임 결과 조회 (승자, 순위, 벌칙 정보)
     */
    @GetMapping("/sessions/{sessionId}/results")
    public ResponseEntity<GameResultsResp> getGameResults(@PathVariable Long sessionId) {
        try {
            log.info("Getting game results: sessionId={}", sessionId);
            GameResultsResp results = quizService.getGameResults(sessionId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error getting game results: sessionId={}", sessionId, e);
            return ResponseEntity.notFound().build();
        }
    }

    // ===================== 호환성 엔드포인트 =====================
    
    /**
     * [BACKWARD COMPATIBILITY] 호환성을 위한 구 경로 지원
     * GET /api/mini-games/quiz/sessions/{sessionId}/current-round
     * → Canonical API로 위임
     */
    @GetMapping("/quiz/sessions/{sessionId}/current-round") 
    public ResponseEntity<?> getCurrentRoundCompat(@PathVariable Long sessionId, HttpServletRequest request) {
        try {
            log.info("[QUIZ-API-COMPAT] GET /quiz/sessions/{}/current-round - redirecting to canonical API", sessionId);
            
            // GameSessionController의 canonical API 호출
            // 여기서는 HTTP 리다이렉션 대신 직접 로직을 수행
            
            // 세션 존재 여부 확인
            if (!quizService.existsSession(sessionId)) {
                log.warn("[QUIZ-API-COMPAT] Session not found: sessionId={}", sessionId);
                return ResponseEntity.status(404)
                    .body(Map.of("code", "SESSION_NOT_FOUND", "message", "세션을 찾을 수 없습니다."));
            }
            
            // 현재 활성 라운드 조회
            Map<String, Object> currentRound = quizService.getCurrentRound(sessionId);
            if (currentRound != null) {
                log.info("[QUIZ-API-COMPAT] Current round found: sessionId={}, roundId={}", sessionId, currentRound.get("roundId"));
                return ResponseEntity.ok(currentRound);
            } else {
                log.info("[QUIZ-API-COMPAT] No active round found: sessionId={}", sessionId);
                return ResponseEntity.status(404)
                    .body(Map.of("code", "ROUND_NOT_FOUND", "message", "현재 진행 중인 라운드가 없습니다."));
            }
            
        } catch (Exception e) {
            log.error("[QUIZ-API-COMPAT] Critical error in getCurrentRoundCompat: sessionId={}", sessionId, e);
            // 500 에러 절대 금지 - 404로 응답
            return ResponseEntity.status(404)
                .body(Map.of("code", "ROUND_NOT_FOUND", "message", "현재 진행 중인 라운드가 없습니다."));
        }
    }

    /**
     * 예외 매핑 (4xx)
     */
    @ExceptionHandler({ IllegalArgumentException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(IllegalArgumentException e) {
        return Map.of("code", "BAD_REQUEST", "message", e.getMessage());
    }
}
