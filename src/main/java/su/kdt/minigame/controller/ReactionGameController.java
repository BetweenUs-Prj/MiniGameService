package su.kdt.minigame.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.domain.ReactionResult;
import su.kdt.minigame.service.ReactionGameService;
import java.util.List;

// DTO를 사용해야 하지만, 설명을 위해 임시로 record 사용
record ReactionSubmitReq(Long userId, double reactionTime) {}

@RestController
@RequestMapping("/api/mini-games/sessions/{sessionId}/reaction")
@RequiredArgsConstructor
public class ReactionGameController {

    private final ReactionGameService reactionGameService;

    // 결과 제출 API
    @PostMapping("/results")
    public ResponseEntity<Void> submitResult(
        @PathVariable Long sessionId,
        @RequestBody ReactionSubmitReq req
    ) {
        reactionGameService.submitResult(sessionId, req.userId(), req.reactionTime());
        return ResponseEntity.ok().build();
    }

    // 결과 조회(리더보드) API
    @GetMapping("/results")
    public ResponseEntity<List<ReactionResult>> getResults(@PathVariable Long sessionId) {
        List<ReactionResult> results = reactionGameService.getResults(sessionId);
        return ResponseEntity.ok(results);
    }
}