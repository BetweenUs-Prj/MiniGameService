package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 추가
import su.kdt.minigame.dto.request.CreateSessionReq;
import su.kdt.minigame.dto.response.SessionResp;

@Service
@RequiredArgsConstructor
public class GameSessionService {

    private final QuizService quizService;
    private final ReactionGameService reactionGameService;

    @Transactional // 추가
    public SessionResp createSession(CreateSessionReq request) {
        // record는 필드명으로 메소드가 자동 생성됩니다.
        String gameType = request.gameType();

        if ("REACTION".equals(gameType)) {
            // ReactionGameService의 세션 생성 메소드를 호출합니다.
            return reactionGameService.createReactionSession(request);

        } else if ("QUIZ".equals(gameType)) {
            // QuizService의 세션 생성 메소드를 호출합니다.
            return quizService.createQuizSession(request);

        } else {
            // 지원하지 않는 게임 타입일 경우 예외를 발생시킵니다.
            throw new IllegalArgumentException("지원하지 않는 게임 타입입니다: " + gameType);
        }
    }
}