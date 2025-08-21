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
@RequestMapping("/api/mini-games")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    /**
     * 특정 퀴즈 게임 세션에 대한 새로운 라운드를 시작합니다.
     * 모든 참여자가 이전 라운드의 답을 제출해야만 시작할 수 있습니다.
     */
    @PostMapping("/sessions/{sessionId}/rounds")
    public ResponseEntity<RoundResp> startRound(@PathVariable Long sessionId, @RequestBody CreateRoundReq request) {
        RoundResp response = quizService.startRound(sessionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 퀴즈 라운드에 대한 사용자의 답변을 제출합니다.
     * 정해진 라운드가 모두 끝나면 게임이 자동으로 종료되고 벌칙이 부여됩니다.
     */
    @PostMapping("/rounds/{roundId}/answers")
    public ResponseEntity<AnswerResp> submitAnswer(@PathVariable Long roundId, @RequestBody SubmitAnswerReq request) {
        AnswerResp response = quizService.submitAnswer(roundId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 페이징 처리된 퀴즈 질문 목록을 조회합니다.
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
