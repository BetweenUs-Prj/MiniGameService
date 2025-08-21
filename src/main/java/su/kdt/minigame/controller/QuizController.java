package su.kdt.minigame.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.dto.request.CreateRoundReq;
import su.kdt.minigame.dto.request.SubmitAnswerReq;
import su.kdt.minigame.dto.response.AnswerResp;
import su.kdt.minigame.dto.response.QuizQuestionResp;
import su.kdt.minigame.dto.response.RoundResp;
import su.kdt.minigame.service.QuizService;

@RestController
@RequestMapping("/api/mini-games") // Base path for all minigame APIs
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    // Session creation is now handled by GameSessionController and has been removed from here.

    /**
     * Starts a new round for a specific quiz game session.
     */
    @PostMapping("/sessions/{sessionId}/rounds")
    public ResponseEntity<RoundResp> startRound(@PathVariable Long sessionId, @RequestBody CreateRoundReq request) {
        RoundResp response = quizService.startRound(sessionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Submits a user's answer for a specific quiz round.
     */
    @PostMapping("/rounds/{roundId}/answers")
    public ResponseEntity<AnswerResp> submitAnswer(@PathVariable Long roundId, @RequestBody SubmitAnswerReq request) {
        AnswerResp response = quizService.submitAnswer(roundId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Ends a quiz game session, triggering penalty assignment.
     */
    @PostMapping("/sessions/{sessionId}/end") // ◀◀◀ NEW API ENDPOINT
    public ResponseEntity<Void> endQuizGame(@PathVariable Long sessionId) {
        quizService.endQuizGame(sessionId);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves a paginated list of available quiz questions.
     */
    @GetMapping("/questions")
    public ResponseEntity<Page<QuizQuestionResp>> getQuestions(
            @RequestParam(required = false) Long placeId,
            @RequestParam(required = false) String category,
            Pageable pageable
    ) {
        Page<QuizQuestionResp> questions = quizService.getQuestions(placeId, category, pageable);
        return ResponseEntity.ok(questions);
    }
}