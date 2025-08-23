package su.kdt.minigame.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.service.NewQuizService;

import java.util.Map;

@RestController
@RequestMapping("/api/mini-games/quiz")
@RequiredArgsConstructor
public class NewQuizController {

    private final NewQuizService quizService;

    @PostMapping("/{sessionId}/rounds")
    public ResponseEntity<NewQuizService.StartRoundRes> startRound(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> body
    ) {
        String category = body.get("category");
        NewQuizService.StartRoundRes result = quizService.startRound(sessionId, category);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/rounds/{roundId}")
    public ResponseEntity<NewQuizService.StartRoundRes> getRound(@PathVariable Long roundId) {
        // 필요 시 roundId로 조회/반환 로직 추가
        return ResponseEntity.status(404).build();
    }
}