package su.kdt.minigame.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import su.kdt.minigame.domain.GamePenalty;
import su.kdt.minigame.domain.GameSession;
import su.kdt.minigame.domain.Penalty;
import su.kdt.minigame.dto.request.CreateSessionReq;
import su.kdt.minigame.dto.response.GamePenaltyResp;
import su.kdt.minigame.dto.response.SessionResp;
import su.kdt.minigame.repository.GamePenaltyRepository;
import su.kdt.minigame.repository.PenaltyRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameSessionServiceTest {

    @Mock private QuizService quizService;
    @Mock private ReactionGameService reactionGameService;
    @Mock private PenaltyRepository penaltyRepository;
    @Mock private GamePenaltyRepository gamePenaltyRepository;

    @InjectMocks
    private GameSessionService gameSessionService;

    private final String userUid = "test-user";

    @Test
    @DisplayName("REACTION 게임 타입으로 세션 생성 시 ReactionGameService를 호출한다")
    void createReactionGameSession() {
        // given
        CreateSessionReq req = new CreateSessionReq(1L, "REACTION", 1L, null);
        Penalty penalty = new Penalty("벌칙 내용", null);
        when(penaltyRepository.findById(1L)).thenReturn(Optional.of(penalty));

        // DTO 직접 생성 대신 mock 사용 → 시그니처 변화에 안전
        SessionResp stubResp = mock(SessionResp.class);
        when(reactionGameService.createReactionSession(eq(req), eq(userUid), any(Penalty.class)))
                .thenReturn(stubResp);

        // when
        SessionResp response = gameSessionService.createSession(req, userUid);

        // then
        verify(reactionGameService).createReactionSession(req, userUid, penalty);
        verify(quizService, never()).createQuizSession(any(), any(), any());
        assertThat(response).isSameAs(stubResp);
    }

    @Test
    @DisplayName("QUIZ 게임 타입으로 세션 생성 시 QuizService를 호출한다")
    void createQuizGameSession() {
        // given
        CreateSessionReq req = new CreateSessionReq(1L, "QUIZ", 1L, 5);
        Penalty penalty = new Penalty("벌칙 내용", null);
        when(penaltyRepository.findById(1L)).thenReturn(Optional.of(penalty));

        SessionResp stubResp = mock(SessionResp.class);
        when(quizService.createQuizSession(eq(req), eq(userUid), any(Penalty.class)))
                .thenReturn(stubResp);

        // when
        SessionResp response = gameSessionService.createSession(req, userUid);

        // then
        verify(quizService).createQuizSession(req, userUid, penalty);
        verify(reactionGameService, never()).createReactionSession(any(), any(), any());
        assertThat(response).isSameAs(stubResp);
    }

    @Test
    @DisplayName("지원하지 않는 게임 타입으로 세션 생성 시 예외가 발생한다")
    void createSessionWithUnsupportedGameType() {
        // given
        CreateSessionReq req = new CreateSessionReq(1L, "UNKNOWN_GAME", 1L, null);
        when(penaltyRepository.findById(1L)).thenReturn(Optional.of(new Penalty("벌칙 내용", null)));

        // when & then
        assertThatThrownBy(() -> gameSessionService.createSession(req, userUid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 게임 타입입니다");
    }

    @Test
    @DisplayName("세션 ID로 게임 벌칙을 정상적으로 조회한다")
    void getGamePenaltySuccessfully() {
        // given
        Long sessionId = 1L;
        GameSession session = mock(GameSession.class);
        Penalty penalty = mock(Penalty.class);
        GamePenalty gamePenalty = new GamePenalty(session, "loser-uid", penalty);

        when(gamePenaltyRepository.findByGameSessionId(sessionId)).thenReturn(Optional.of(gamePenalty));
        when(penalty.getDescription()).thenReturn("벌칙!");

        // when
        GamePenaltyResp response = gameSessionService.getGamePenalty(sessionId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.loserUid()).isEqualTo("loser-uid");
        assertThat(response.penaltyDescription()).isEqualTo("벌칙!");
    }

    @Test
    @DisplayName("결정되지 않은 게임 벌칙 조회 시 예외가 발생한다")
    void getGamePenaltyNotFound() {
        // given
        when(gamePenaltyRepository.findByGameSessionId(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> gameSessionService.getGamePenalty(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("아직 해당 게임의 벌칙이 결정되지 않았습니다.");
    }
}
