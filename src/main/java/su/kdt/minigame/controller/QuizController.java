package su.kdt.minigame.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.dto.request.CreateRoundReq;
import su.kdt.minigame.dto.request.CreateSessionReq;
import su.kdt.minigame.dto.request.SubmitAnswerReq;
import su.kdt.minigame.dto.response.AnswerResp;
import su.kdt.minigame.dto.response.QuizQuestionResp;
import su.kdt.minigame.dto.response.RoundResp;
import su.kdt.minigame.dto.response.SessionResp;
import su.kdt.minigame.service.QuizService;

import java.net.URI;

@RestController
@RequestMapping("/api/mini-games") // 모든 API는 /api/mini-games로 시작
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    // (세션 생성, 라운드 시작, 답안 제출 API는 동일)
    @PostMapping("/sessions")
    public ResponseEntity<SessionResp> createSession(@RequestBody CreateSessionReq request) {
        SessionResp response = quizService.createQuizSession(request);
        return ResponseEntity.created(URI.create("/api/mini-games/sessions/" + response.sessionId()))
                .body(response);
    }

    @PostMapping("/sessions/{sessionId}/rounds")
    public ResponseEntity<RoundResp> startRound(@PathVariable Long sessionId, @RequestBody CreateRoundReq request) {
        RoundResp response = quizService.startRound(sessionId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rounds/{roundId}/answers")
    public ResponseEntity<AnswerResp> submitAnswer(@PathVariable Long roundId, @RequestBody SubmitAnswerReq request) {
        AnswerResp response = quizService.submitAnswer(roundId, request);
        return ResponseEntity.ok(response);
    }

    // 질문 목록 API의 정확한 주소는 "/questions" 입니다.
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