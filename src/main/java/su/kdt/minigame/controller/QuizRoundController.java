package su.kdt.minigame.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.dto.StartRoundReq;
import su.kdt.minigame.dto.StartRoundRes;
import su.kdt.minigame.service.QuizService;

@RestController
@RequestMapping("/api/mini-games/quiz")
@RequiredArgsConstructor
public class QuizRoundController {

    private final QuizService quizService;

    @PostMapping("/{sessionId}/rounds")
    public ResponseEntity<StartRoundRes> start(@PathVariable Long sessionId, @RequestBody StartRoundReq req) {
        Long roundId = quizService.startRound(sessionId, req.getCategory());
        return ResponseEntity.ok(new StartRoundRes(roundId));
    }
}