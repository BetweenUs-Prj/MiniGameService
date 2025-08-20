package su.kdt.minigame.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.dto.request.CreateSessionReq;
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
     * @param userUid 요청을 보낸 사용자의 ID (HTTP 헤더에서 추출)
     * @param request 게임 타입과 약속 ID 정보
     * @return 생성된 세션 정보
     */
    @PostMapping
    public ResponseEntity<SessionResp> createSession(
            @RequestHeader("X-USER-UID") String userUid, // ◀◀◀ 이 부분이 추가되었습니다.
            @RequestBody CreateSessionReq request
    ) {
        // 서비스 호출 시, 방장의 ID인 userUid를 함께 전달합니다.
        SessionResp response = gameSessionService.createSession(request, userUid);
        
        // 생성된 리소스의 위치를 Location 헤더에 담아 201 Created 응답을 반환합니다.
        return ResponseEntity.created(URI.create("/api/mini-games/sessions/" + response.sessionId()))
                .body(response);
    }
}