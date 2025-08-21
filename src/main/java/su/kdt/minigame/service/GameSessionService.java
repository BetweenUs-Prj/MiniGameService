package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su.kdt.minigame.domain.GamePenalty;
import su.kdt.minigame.domain.Penalty;
import su.kdt.minigame.dto.request.CreateSessionReq;
import su.kdt.minigame.dto.response.GamePenaltyResp;
import su.kdt.minigame.dto.response.SessionResp;
import su.kdt.minigame.repository.GamePenaltyRepository;
import su.kdt.minigame.repository.PenaltyRepository;

@Service
@RequiredArgsConstructor
public class GameSessionService {

    private final QuizService quizService;
    private final ReactionGameService reactionGameService;
    private final PenaltyRepository penaltyRepository;
    private final GamePenaltyRepository gamePenaltyRepository;

    @Transactional
    public SessionResp createSession(CreateSessionReq request, String userUid) {
        Penalty selectedPenalty = penaltyRepository.findById(request.penaltyId())
                .orElseThrow(() -> new IllegalArgumentException("Penalty not found: " + request.penaltyId()));

        String gameType = request.gameType();

        if ("REACTION".equals(gameType)) {
            return reactionGameService.createReactionSession(request, userUid, selectedPenalty);

        } else if ("QUIZ".equals(gameType)) {
            // ◀◀◀ 이 부분에 selectedPenalty를 전달해야 합니다.
            return quizService.createQuizSession(request, userUid, selectedPenalty);

        } else {
            throw new IllegalArgumentException("지원하지 않는 게임 타입입니다: " + gameType);
        }
    }

    @Transactional(readOnly = true)
    public GamePenaltyResp getGamePenalty(Long sessionId) {
        GamePenalty gamePenalty = gamePenaltyRepository.findByGameSessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("아직 해당 게임의 벌칙이 결정되지 않았습니다."));
        
        return GamePenaltyResp.from(gamePenalty);
    }
}