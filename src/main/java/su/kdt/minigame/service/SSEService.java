package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SSEService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 세션별 일반 구독자
    private final Map<Long, Map<String, SseEmitter>> sessionSubscribers = new ConcurrentHashMap<>();
    
    // 반응속도 게임 전용 구독자
    private final Map<Long, Map<String, SseEmitter>> reactionSubscribers = new ConcurrentHashMap<>();
    
    // 퀴즈 게임 전용 구독자
    private final Map<Long, Map<String, SseEmitter>> quizSubscribers = new ConcurrentHashMap<>();

    public void addSubscriber(Long sessionId, String userUid, SseEmitter emitter) {
        sessionSubscribers.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                          .put(userUid, emitter);
        log.info("[SSE] Added subscriber {} to session {}", userUid, sessionId);
    }

    public void removeSubscriber(Long sessionId, String userUid) {
        Map<String, SseEmitter> subscribers = sessionSubscribers.get(sessionId);
        if (subscribers != null) {
            subscribers.remove(userUid);
            if (subscribers.isEmpty()) {
                sessionSubscribers.remove(sessionId);
            }
        }
        log.info("[SSE] Removed subscriber {} from session {}", userUid, sessionId);
    }

    public void addReactionSubscriber(Long sessionId, String userUid, SseEmitter emitter) {
        reactionSubscribers.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                          .put(userUid, emitter);
        log.info("[SSE] Added reaction subscriber {} to session {}", userUid, sessionId);
    }

    public void removeReactionSubscriber(Long sessionId, String userUid) {
        Map<String, SseEmitter> subscribers = reactionSubscribers.get(sessionId);
        if (subscribers != null) {
            subscribers.remove(userUid);
            if (subscribers.isEmpty()) {
                reactionSubscribers.remove(sessionId);
            }
        }
        log.info("[SSE] Removed reaction subscriber {} from session {}", userUid, sessionId);
    }

    public void addQuizSubscriber(Long sessionId, String userUid, SseEmitter emitter) {
        quizSubscribers.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                      .put(userUid, emitter);
        log.info("[SSE] Added quiz subscriber {} to session {}", userUid, sessionId);
    }

    public void removeQuizSubscriber(Long sessionId, String userUid) {
        Map<String, SseEmitter> subscribers = quizSubscribers.get(sessionId);
        if (subscribers != null) {
            subscribers.remove(userUid);
            if (subscribers.isEmpty()) {
                quizSubscribers.remove(sessionId);
            }
        }
        log.info("[SSE] Removed quiz subscriber {} from session {}", userUid, sessionId);
    }

    public void broadcastToSession(Long sessionId, String eventName, Object data) {
        Map<String, SseEmitter> subscribers = sessionSubscribers.get(sessionId);
        if (subscribers != null && !subscribers.isEmpty()) {
            broadcastToSubscribers(subscribers, eventName, data, "session-" + sessionId);
        }
    }

    public void broadcastToReactionGame(Long sessionId, String eventName, Object data) {
        Map<String, SseEmitter> subscribers = reactionSubscribers.get(sessionId);
        if (subscribers != null && !subscribers.isEmpty()) {
            broadcastToSubscribers(subscribers, eventName, data, "reaction-" + sessionId);
        }
    }

    public void broadcastToQuizGame(Long sessionId, String eventName, Object data) {
        Map<String, SseEmitter> subscribers = quizSubscribers.get(sessionId);
        if (subscribers != null && !subscribers.isEmpty()) {
            broadcastToSubscribers(subscribers, eventName, data, "quiz-" + sessionId);
        }
    }

    private void broadcastToSubscribers(Map<String, SseEmitter> subscribers, String eventName, Object data, String logPrefix) {
        subscribers.entrySet().removeIf(entry -> {
            String userUid = entry.getKey();
            SseEmitter emitter = entry.getValue();
            
            try {
                String jsonData = objectMapper.writeValueAsString(data);
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(jsonData));
                return false; // 성공시 제거하지 않음
            } catch (IOException e) {
                log.warn("[SSE] Failed to send {} event to user {}: {}", logPrefix, userUid, e.getMessage());
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {}
                return true; // 실패시 제거
            }
        });
        
        log.debug("[SSE] Broadcasted {} event to {} subscribers", logPrefix, subscribers.size());
    }

    public void sendToUser(Long sessionId, String userUid, String eventName, Object data) {
        // 일반 세션 구독자에서 찾기
        Map<String, SseEmitter> subscribers = sessionSubscribers.get(sessionId);
        SseEmitter emitter = subscribers != null ? subscribers.get(userUid) : null;
        
        if (emitter != null) {
            sendToEmitter(emitter, userUid, eventName, data);
            return;
        }
        
        // 반응속도 게임 구독자에서 찾기
        subscribers = reactionSubscribers.get(sessionId);
        emitter = subscribers != null ? subscribers.get(userUid) : null;
        
        if (emitter != null) {
            sendToEmitter(emitter, userUid, eventName, data);
            return;
        }
        
        // 퀴즈 게임 구독자에서 찾기
        subscribers = quizSubscribers.get(sessionId);
        emitter = subscribers != null ? subscribers.get(userUid) : null;
        
        if (emitter != null) {
            sendToEmitter(emitter, userUid, eventName, data);
        } else {
            log.warn("[SSE] User {} not found in session {}", userUid, sessionId);
        }
    }

    private void sendToEmitter(SseEmitter emitter, String userUid, String eventName, Object data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(jsonData));
        } catch (IOException e) {
            log.warn("[SSE] Failed to send event to user {}: {}", userUid, e.getMessage());
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {}
        }
    }

    public Map<String, Object> getSessionStatus(Long sessionId) {
        int generalCount = sessionSubscribers.getOrDefault(sessionId, Map.of()).size();
        int reactionCount = reactionSubscribers.getOrDefault(sessionId, Map.of()).size();
        int quizCount = quizSubscribers.getOrDefault(sessionId, Map.of()).size();
        
        return Map.of(
            "sessionId", sessionId,
            "subscribers", Map.of(
                "general", generalCount,
                "reaction", reactionCount,
                "quiz", quizCount,
                "total", generalCount + reactionCount + quizCount
            ),
            "timestamp", Instant.now().toEpochMilli()
        );
    }

    // 하트비트 전송
    public void sendHeartbeat() {
        sendHeartbeatToSubscribers(sessionSubscribers, "general");
        sendHeartbeatToSubscribers(reactionSubscribers, "reaction");
        sendHeartbeatToSubscribers(quizSubscribers, "quiz");
    }

    private void sendHeartbeatToSubscribers(Map<Long, Map<String, SseEmitter>> allSubscribers, String type) {
        allSubscribers.forEach((sessionId, subscribers) -> {
            subscribers.entrySet().removeIf(entry -> {
                try {
                    entry.getValue().send(SseEmitter.event()
                            .name("heartbeat")
                            .data("{ \"type\": \"" + type + "\", \"timestamp\": " + Instant.now().toEpochMilli() + " }"));
                    return false;
                } catch (IOException e) {
                    log.debug("[SSE] Heartbeat failed for user {} in {} session {}", entry.getKey(), type, sessionId);
                    return true;
                }
            });
        });
    }
}