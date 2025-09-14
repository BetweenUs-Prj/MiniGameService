package su.kdt.minigame.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import su.kdt.minigame.service.SSEService;
import su.kdt.minigame.support.UidResolverFilter;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SSEController {

    private final SSEService sseService;

    @GetMapping(value = "/sessions/{sessionId}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToSession(
            @PathVariable Long sessionId,
            HttpServletRequest request) {
        
        String userUid = (String) request.getAttribute(UidResolverFilter.ATTR_UID);
        
        log.info("[SSE] User {} subscribing to session {}", userUid, sessionId);
        
        SseEmitter emitter = new SseEmitter(600_000L); // 10분 타임아웃
        
        try {
            sseService.addSubscriber(sessionId, userUid, emitter);
            
            // 초기 연결 확인 메시지
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{ \"status\": \"connected\", \"sessionId\": " + sessionId + " }"));
                    
        } catch (IOException e) {
            log.error("[SSE] Failed to send initial message to user {} in session {}", userUid, sessionId, e);
            emitter.completeWithError(e);
        }
        
        emitter.onCompletion(() -> {
            log.info("[SSE] Connection completed for user {} in session {}", userUid, sessionId);
            sseService.removeSubscriber(sessionId, userUid);
        });
        
        emitter.onTimeout(() -> {
            log.info("[SSE] Connection timeout for user {} in session {}", userUid, sessionId);
            sseService.removeSubscriber(sessionId, userUid);
        });
        
        emitter.onError(error -> {
            log.error("[SSE] Connection error for user {} in session {}", userUid, sessionId, error);
            sseService.removeSubscriber(sessionId, userUid);
        });
        
        return emitter;
    }

    @GetMapping(value = "/reaction/{sessionId}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToReactionGame(
            @PathVariable Long sessionId,
            HttpServletRequest request) {
        
        String userUid = (String) request.getAttribute(UidResolverFilter.ATTR_UID);
        
        log.info("[SSE] User {} subscribing to reaction game {}", userUid, sessionId);
        
        SseEmitter emitter = new SseEmitter(600_000L); // 10분 타임아웃
        
        try {
            sseService.addReactionSubscriber(sessionId, userUid, emitter);
            
            emitter.send(SseEmitter.event()
                    .name("reaction-connected")
                    .data("{ \"status\": \"connected\", \"sessionId\": " + sessionId + ", \"type\": \"reaction\" }"));
                    
        } catch (IOException e) {
            log.error("[SSE] Failed to send initial reaction message to user {} in session {}", userUid, sessionId, e);
            emitter.completeWithError(e);
        }
        
        emitter.onCompletion(() -> sseService.removeReactionSubscriber(sessionId, userUid));
        emitter.onTimeout(() -> sseService.removeReactionSubscriber(sessionId, userUid));
        emitter.onError(error -> sseService.removeReactionSubscriber(sessionId, userUid));
        
        return emitter;
    }

    @GetMapping(value = "/quiz/{sessionId}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToQuizGame(
            @PathVariable Long sessionId,
            HttpServletRequest request) {
        
        String userUid = (String) request.getAttribute(UidResolverFilter.ATTR_UID);
        
        log.info("[SSE] User {} subscribing to quiz game {}", userUid, sessionId);
        
        SseEmitter emitter = new SseEmitter(600_000L); // 10분 타임아웃
        
        try {
            sseService.addQuizSubscriber(sessionId, userUid, emitter);
            
            emitter.send(SseEmitter.event()
                    .name("quiz-connected")
                    .data("{ \"status\": \"connected\", \"sessionId\": " + sessionId + ", \"type\": \"quiz\" }"));
                    
        } catch (IOException e) {
            log.error("[SSE] Failed to send initial quiz message to user {} in session {}", userUid, sessionId, e);
            emitter.completeWithError(e);
        }
        
        emitter.onCompletion(() -> sseService.removeQuizSubscriber(sessionId, userUid));
        emitter.onTimeout(() -> sseService.removeQuizSubscriber(sessionId, userUid));
        emitter.onError(error -> sseService.removeQuizSubscriber(sessionId, userUid));
        
        return emitter;
    }

    @GetMapping("/sessions/{sessionId}/status")
    public ResponseEntity<Object> getSessionStatus(@PathVariable Long sessionId) {
        try {
            var status = sseService.getSessionStatus(sessionId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{ \"error\": \"" + e.getMessage() + "\" }");
        }
    }
}