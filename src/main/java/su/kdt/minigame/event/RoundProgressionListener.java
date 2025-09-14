package su.kdt.minigame.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import su.kdt.minigame.service.QuizService;

/**
 * 라운드 진행 이벤트 리스너 - 트랜잭션 커밋 후 별도 트랜잭션에서 다음 라운드 처리
 */
//@Component  // 비활성화 - QuizService에서 직접 처리
@RequiredArgsConstructor
@Slf4j
public class RoundProgressionListener {

    private final QuizService quizService;

    /**
     * 라운드 완료 후 다음 라운드를 새로운 트랜잭션에서 생성
     * 트랜잭션 커밋 후 실행되므로 가시성 문제 해결
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRoundCompleted(RoundCompletedEvent event) {
        log.info("[EVENT] RoundCompleted received: sessionId={}, roundId={}, roundNo={}", 
                event.sessionId(), event.roundId(), event.roundNo());
        
        try {
            startNextRoundInNewTransaction(event.sessionId(), event.roundNo(), event.totalPlayers());
        } catch (Exception e) {
            log.error("[EVENT] Failed to handle RoundCompleted: sessionId={}, error={}", 
                    event.sessionId(), e.getMessage(), e);
        }
    }

    /**
     * 새로운 트랜잭션에서 다음 라운드 시작
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startNextRoundInNewTransaction(Long sessionId, int completedRoundNo, int totalPlayers) {
        log.info("[EVENT] Starting next round in new transaction: sessionId={}, completedRound={}", 
                sessionId, completedRoundNo);
        
        try {
            quizService.startRoundForSession(sessionId);
            log.info("[EVENT] Successfully started next round: sessionId={}", sessionId);
        } catch (Exception e) {
            log.warn("[EVENT] Failed to start next round: sessionId={}, error={}, will retry with scheduler", 
                    sessionId, e.getMessage());
            
            // 실패 시 기존 스케줄러로 폴백
            try {
                quizService.scheduleNextRound(sessionId);
            } catch (Exception retryError) {
                log.error("[EVENT] Failed to schedule next round as fallback: sessionId={}, error={}", 
                        sessionId, retryError.getMessage());
            }
        }
    }
}