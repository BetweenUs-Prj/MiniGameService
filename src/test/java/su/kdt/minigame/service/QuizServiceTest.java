package su.kdt.minigame.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import su.kdt.minigame.domain.*;
import su.kdt.minigame.dto.request.SubmitAnswerReq;
import su.kdt.minigame.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // 불필요 stubbing 예외 방지
@DisplayName("QuizService 테스트")
class QuizServiceTest {

    @InjectMocks
    private QuizService quizService;

    @Mock private GameRepo gameRepo;
    @Mock private QuizRoundRepo roundRepo;
    @Mock private QuizAnswerRepository answerRepo;
    @Mock private QuizQuestionOptionRepo optionRepo;
    @Mock private GamePenaltyRepository gamePenaltyRepository;
    @Mock private PenaltyRepository penaltyRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @Mock private GameSession quizSession;
    @Mock private Penalty penalty;
    @Mock private QuizQuestion question;
    @Mock private QuizRound round;

    @Nested
    @DisplayName("답변 제출")
    class SubmitAnswer {

        @Test
        @DisplayName("마지막 라운드, 모든 답변 완료 시 게임 종료 및 벌칙 부여")
        void submitAnswer_And_FinishGame() {
            // GIVEN
            long sessionId = 1L;
            long roundId = 1L;
            long penaltyId = 10L;
            SubmitAnswerReq req = new SubmitAnswerReq("user1", "정답");

            when(roundRepo.findById(roundId)).thenReturn(Optional.of(round));
            when(round.getSession()).thenReturn(quizSession);
            when(round.getQuestion()).thenReturn(question);

            // 옵션은 mock으로 구성
            QuizQuestionOption correctOption = mock(QuizQuestionOption.class);
            when(correctOption.isCorrect()).thenReturn(true);
            when(correctOption.getOptionText()).thenReturn("정답");
            when(correctOption.getQuestion()).thenReturn(question);
            when(optionRepo.findByQuestion(question)).thenReturn(List.of(correctOption));

            when(round.getStartTime()).thenReturn(LocalDateTime.now().minusSeconds(1));

            when(answerRepo.countDistinctUserUidsByRound(round)).thenReturn(2L);
            when(roundRepo.countBySession(quizSession)).thenReturn(5L);
            when(quizSession.getTotalRounds()).thenReturn(5);
            when(round.getStatus()).thenReturn(QuizRound.Status.COMPLETED);

            when(quizSession.getId()).thenReturn(sessionId);
            when(quizSession.getSelectedPenaltyId()).thenReturn(penaltyId);
            when(penaltyRepository.findById(penaltyId)).thenReturn(Optional.of(penalty));
            when(penalty.getDescription()).thenReturn("테스트 벌칙");

            // 점수 차이: user1이 패배
            when(answerRepo.countCorrectAnswersByUser(sessionId, "user1")).thenReturn(3L);
            when(answerRepo.countCorrectAnswersByUser(sessionId, "user2")).thenReturn(5L);

            // WHEN
            quizService.submitAnswer(roundId, req);

            // THEN
            ArgumentCaptor<GamePenalty> penaltyCaptor = ArgumentCaptor.forClass(GamePenalty.class);
            verify(gamePenaltyRepository).save(penaltyCaptor.capture());
            GamePenalty capturedPenalty = penaltyCaptor.getValue();

            assertThat(capturedPenalty).isNotNull();
            assertThat(capturedPenalty.getUserUid()).isEqualTo("user1");

            // convertAndSend 오버로드 모호성 방지
            verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("존재하지 않는 라운드에 답변 제출 시 예외가 발생한다")
        void submitAnswerToNonExistentRound() {
            when(roundRepo.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> quizService.submitAnswer(999L, new SubmitAnswerReq("u", "a")))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
