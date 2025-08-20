package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su.kdt.minigame.domain.*;
import su.kdt.minigame.dto.request.CreateSessionReq;
import su.kdt.minigame.dto.response.SessionResp;
import su.kdt.minigame.repository.GamePenaltyRepository;
import su.kdt.minigame.repository.GameRepo;
import su.kdt.minigame.repository.PenaltyRepository;
import su.kdt.minigame.repository.ReactionResultRepo;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReactionGameService {

    private final GameRepo gameRepo;
    private final ReactionResultRepo reactionResultRepo;
    private final GamePenaltyRepository gamePenaltyRepository;
    // PenaltyRepository는 이제 여기서 직접 사용하지 않으므로 제거해도 됩니다.

    @Transactional
    public SessionResp createReactionSession(CreateSessionReq request, String userUid, Penalty selectedPenalty) {
        GameSession session = new GameSession(request.appointmentId(), GameSession.GameType.REACTION, userUid, selectedPenalty);
        GameSession savedSession = gameRepo.save(session);
        return SessionResp.from(savedSession);
    }

    @Transactional
    public void submitResult(Long sessionId, String userUid, double reactionTime) {
        GameSession session = gameRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (session.getGameType() != GameSession.GameType.REACTION) {
            throw new IllegalStateException("Not a reaction game session");
        }

        ReactionResult result = new ReactionResult(session, userUid, reactionTime);
        reactionResultRepo.save(result);

        // TODO: 실제 약속(appointment)의 참여자 수를 동적으로 가져오는 로직이 필요합니다.
        int totalPlayers = 2; // Placeholder
        List<ReactionResult> currentResults = reactionResultRepo.findBySession(session);

        if (currentResults.size() >= totalPlayers) {
            assignPenalty(session, currentResults);
        }
    }

    private void assignPenalty(GameSession session, List<ReactionResult> results) {
        ReactionResult loserResult = Collections.max(results, Comparator.comparing(ReactionResult::getReactionTime));
        String loserUid = loserResult.getUserUid();

        // 5. 게임 세션에 미리 정해진 벌칙을 바로 가져옵니다. (로직 단순화)
        Penalty selectedPenalty = session.getSelectedPenalty();
        if (selectedPenalty == null) {
            throw new IllegalStateException("게임에 미리 선택된 벌칙이 없습니다.");
        }
        
        // 6. 벌칙 결과 저장
        GamePenalty gamePenalty = new GamePenalty(session, loserUid, selectedPenalty);
        gamePenaltyRepository.save(gamePenalty);

        // 7. 게임 세션 상태 '종료'로 변경
        session.finish();
    }

    @Transactional(readOnly = true)
    public List<ReactionResult> getResults(Long sessionId) {
        GameSession session = gameRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        return reactionResultRepo.findBySessionOrderByReactionTimeAsc(session);
    }
}