package su.kdt.minigame.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.dto.request.CreateSessionReq;
import su.kdt.minigame.dto.response.GamePenaltyResp;
import su.kdt.minigame.dto.response.SessionResp;
import su.kdt.minigame.service.GameSessionService;

import java.net.URI;

@RestController
@RequestMapping("/api/mini-games/sessions")
@RequiredArgsConstructor
public class GameSessionController {

    private final GameSessionService gameSessionService;

    /**
     * 새로운 게임 세션을 생성합니다.
     */
    @PostMapping
    public ResponseEntity<SessionResp> createSession(
            @RequestHeader("X-USER-UID") String userUid,
            @RequestBody CreateSessionReq request
    ) {
        SessionResp response = gameSessionService.createSession(request, userUid);
        return ResponseEntity.created(URI.create("/api/mini-games/sessions/" + response.sessionId()))
                .body(response);
    }

    /**
     * 특정 게임 세션에 할당된 벌칙 결과를 조회합니다.
     */
    @GetMapping("/{sessionId}/penalty")
    public ResponseEntity<GamePenaltyResp> getPenaltyForSession(@PathVariable Long sessionId) {
        GamePenaltyResp response = gameSessionService.getGamePenalty(sessionId);
        return ResponseEntity.ok(response);
    }
}