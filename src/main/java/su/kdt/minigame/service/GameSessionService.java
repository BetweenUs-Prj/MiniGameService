package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su.kdt.minigame.domain.Penalty;
import su.kdt.minigame.dto.request.CreateSessionReq;
import su.kdt.minigame.dto.response.SessionResp;
import su.kdt.minigame.repository.PenaltyRepository;

@Service
@RequiredArgsConstructor
public class GameSessionService {

    private final QuizService quizService;
    private final ReactionGameService reactionGameService;
    private final PenaltyRepository penaltyRepository;

    @Transactional
    public SessionResp createSession(CreateSessionReq request, String userUid) {
        // 1. 요청받은 penaltyId로 Penalty 엔티티를 조회합니다.
        Penalty selectedPenalty = penaltyRepository.findById(request.penaltyId())
                .orElseThrow(() -> new IllegalArgumentException("Penalty not found: " + request.penaltyId()));

        String gameType = request.gameType();

        if ("REACTION".equals(gameType)) {
            // 2. 조회한 Penalty 객체를 생성자에 함께 전달합니다.
            return reactionGameService.createReactionSession(request, userUid, selectedPenalty);

        } else if ("QUIZ".equals(gameType)) {
            // 퀴즈 서비스도 동일하게 수정 필요
            return quizService.createQuizSession(request, userUid, selectedPenalty);

        } else {
            // 'new' 키워드를 추가하여 예외 객체를 생성합니다.
            throw new IllegalArgumentException("지원하지 않는 게임 타입입니다: " + gameType);
        }
    }
}