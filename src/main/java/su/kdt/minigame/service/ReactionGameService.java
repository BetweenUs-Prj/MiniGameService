package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReactionGameService {

    private final GameRepo gameRepo;
    private final ReactionResultRepo reactionResultRepo;
    private final GamePenaltyRepository gamePenaltyRepository;
    private final PenaltyRepository penaltyRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public SessionResp createReactionSession(CreateSessionReq request, String userUid, Penalty selectedPenalty) {
        // ✅ GameSession 생성자 시그니처: (Long appointmentId, GameType, String hostUid, Long penaltyId, Integer totalRounds)
        GameSession session = new GameSession(
                request.appointmentId(),
                GameSession.GameType.REACTION,
                userUid,
                selectedPenalty.getId(),
                1   // 리액션 게임은 1라운드로 고정
        );
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

        String destination = "/topic/game/" + sessionId;
        Map<String, Object> messagePayload = Map.of(
                "type", "RESULT_SUBMITTED",
                "userUid", userUid,
                "reactionTime", reactionTime
        );
        messagingTemplate.convertAndSend(destination, messagePayload);

        // TODO: 약속 참가자 수 동적 조회 필요
        int totalPlayers = 2; // Placeholder
        List<ReactionResult> currentResults = reactionResultRepo.findBySession(session);

        if (currentResults.size() >= totalPlayers) {
            assignPenalty(session, currentResults);
        }
    }

    private void assignPenalty(GameSession session, List<ReactionResult> results) {
        // 반응 시간이 가장 느린(값이 큰) 사용자가 패자
        ReactionResult loserResult = Collections.max(results, Comparator.comparing(ReactionResult::getReactionTime));
        String loserUid = loserResult.getUserUid();

        Long penaltyId = session.getSelectedPenaltyId();
        Penalty selectedPenalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new IllegalStateException("Selected penalty not found in DB: " + penaltyId));

        GamePenalty gamePenalty = new GamePenalty(session, loserUid, selectedPenalty);
        gamePenaltyRepository.save(gamePenalty);

        session.finish(selectedPenalty.getDescription());

        String destination = "/topic/game/" + session.getId();
        Map<String, Object> finalResultMessage = Map.of(
                "type", "GAME_FINISHED",
                "loserUid", loserUid,
                "penalty", selectedPenalty.getDescription()
        );
        messagingTemplate.convertAndSend(destination, finalResultMessage);
    }

    @Transactional(readOnly = true)
    public List<ReactionResult> getResults(Long sessionId) {
        GameSession session = gameRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        return reactionResultRepo.findBySessionOrderByReactionTimeAsc(session);
    }
}
