package su.kdt.minigame.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.dto.request.CreateSessionReq;
import su.kdt.minigame.dto.response.SessionResp;
import su.kdt.minigame.service.GameSessionService;
import java.net.URI; // URI import 추가

@RestController
@RequestMapping("/api/mini-games/sessions")
@RequiredArgsConstructor
public class GameSessionController {

    private final GameSessionService gameSessionService;

    @PostMapping
    public ResponseEntity<SessionResp> createSession(@RequestBody CreateSessionReq request) {
        SessionResp response = gameSessionService.createSession(request);
        
        // ===== 아래 return 구문을 추가하면 해결됩니다! =====
        // 생성된 세션의 URI와 함께 201 Created 상태를 반환합니다.
        return ResponseEntity.created(URI.create("/api/mini-games/sessions/" + response.sessionId()))
                .body(response);
        // ===== 여기까지 추가 =====
    }
}