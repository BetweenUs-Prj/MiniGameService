package su.kdt.minigame.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.domain.ReactionResult;
import su.kdt.minigame.service.ReactionGameService;

import java.util.List;
import java.util.stream.Collectors;

// Request DTO for submitting reaction time
record ReactionTimeReq(double reactionTime) {}

// Response DTO for displaying results
record ReactionResultResp(String userUid, double reactionTime, int ranking) {
    public static ReactionResultResp from(ReactionResult result) {
        return new ReactionResultResp(
                result.getUserUid(),
                result.getReactionTime(),
                result.getRanking()
        );
    }
}

@RestController
@RequestMapping("/api/mini-games/sessions/{sessionId}/reaction")
@RequiredArgsConstructor
public class ReactionGameController {

    private final ReactionGameService reactionGameService;

    /**
     * Submits a user's reaction time result for a specific game session.
     * @param userUid The ID of the user submitting the result (from header).
     */
    @PostMapping("/results")
    public ResponseEntity<Void> submitResult(
            @PathVariable Long sessionId,
            @RequestHeader("X-USER-UID") String userUid, // Get user ID from header
            @RequestBody ReactionTimeReq req
    ) {
        reactionGameService.submitResult(sessionId, userUid, req.reactionTime());
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves the leaderboard for a specific game session.
     */
    @GetMapping("/results")
    public ResponseEntity<List<ReactionResultResp>> getResults(@PathVariable Long sessionId) {
        List<ReactionResult> results = reactionGameService.getResults(sessionId);
        
        // Convert entities to DTOs before sending the response
        List<ReactionResultResp> response = results.stream()
                .map(ReactionResultResp::from)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(response);
    }
}