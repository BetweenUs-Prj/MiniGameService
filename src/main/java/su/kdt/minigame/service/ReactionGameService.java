package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su.kdt.minigame.domain.GameSession;
import su.kdt.minigame.domain.ReactionResult;
import su.kdt.minigame.dto.request.CreateSessionReq; // 추가
import su.kdt.minigame.dto.response.SessionResp;   // 추가
import su.kdt.minigame.repository.GameRepo;
import su.kdt.minigame.repository.ReactionResultRepo;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReactionGameService {

    private final GameRepo gameRepo;
    private final ReactionResultRepo reactionResultRepo;

    // ===== 아래 메소드를 새로 추가해주세요! =====
    @Transactional
    public SessionResp createReactionSession(CreateSessionReq request) {
        GameSession session = new GameSession(
                request.appointmentId(),
                GameSession.GameType.REACTION // 타입을 REACTION으로 지정
        );
        GameSession savedSession = gameRepo.save(session);
        return SessionResp.from(savedSession);
    }
    // ===== 여기까지 추가 =====

    // 사용자의 반응 속도 결과를 제출받아 저장합니다.
    @Transactional
    public void submitResult(Long sessionId, Long userId, double reactionTime) {
        GameSession session = gameRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (session.getGameType() != GameSession.GameType.REACTION) {
            throw new IllegalStateException("Not a reaction game session");
        }

        ReactionResult result = new ReactionResult();
        result.setSession(session);
        result.setUserId(userId);
        result.setReactionTime(reactionTime);

        reactionResultRepo.save(result);
    }

    // 특정 게임 세션의 결과 목록(리더보드)을 조회합니다.
    @Transactional(readOnly = true)
    public List<ReactionResult> getResults(Long sessionId) {
        GameSession session = gameRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        return reactionResultRepo.findBySessionOrderByReactionTimeAsc(session);
    }
}