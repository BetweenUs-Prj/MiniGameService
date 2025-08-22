package su.kdt.minigame.service;

import org.junit.jupiter.api.DisplayName;
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
import su.kdt.minigame.dto.request.CreateSessionReq;
import su.kdt.minigame.repository.GamePenaltyRepository;
import su.kdt.minigame.repository.GameRepo;
import su.kdt.minigame.repository.PenaltyRepository;
import su.kdt.minigame.repository.ReactionResultRepo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReactionGameServiceTest {

    @Mock private GameRepo gameRepo;
    @Mock private ReactionResultRepo reactionResultRepo;
    @Mock private GamePenaltyRepository gamePenaltyRepository;
    @Mock private PenaltyRepository penaltyRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ReactionGameService reactionGameService;

    @Test
    @DisplayName("리액션 게임 세션을 생성하면 totalRounds가 1로 고정된다")
    void createReactionSession() {
        // given
        CreateSessionReq req = new CreateSessionReq(1L, "REACTION", 1L, null);
        Penalty penalty = mock(Penalty.class);
        when(gameRepo.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        reactionGameService.createReactionSession(req, "host-user", penalty);

        // then
        ArgumentCaptor<GameSession> captor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameRepo).save(captor.capture());
        GameSession savedSession = captor.getValue();

        assertThat(savedSession.getGameType()).isEqualTo(GameSession.GameType.REACTION);
        assertThat(savedSession.getTotalRounds()).isEqualTo(1);
    }

    @Test
@DisplayName("결과 제출 시 모든 참여자가 제출 완료하면 벌칙이 배정된다")
void submitResultAndAssignPenalty() {
    Long sessionId = 1L;
    GameSession session = mock(GameSession.class);
    when(session.getId()).thenReturn(sessionId);
    when(session.getSelectedPenaltyId()).thenReturn(10L);
    when(session.getGameType()).thenReturn(GameSession.GameType.REACTION);
    when(gameRepo.findById(sessionId)).thenReturn(Optional.of(session));

    when(reactionResultRepo.save(any(ReactionResult.class))).thenAnswer(inv -> inv.getArgument(0));

    ReactionResult resultFast = new ReactionResult(session, "user-fast", 100.5);
    ReactionResult resultSlow = new ReactionResult(session, "user-slow", 200.8);

    // ✅ 서비스는 findBySession을 한 번만 호출하므로, 호출 시점에는 이미 두 명이 있다고 스텁
    when(reactionResultRepo.findBySession(session))
            .thenReturn(java.util.List.of(resultFast, resultSlow));

    Penalty penalty = new Penalty("패배 벌칙", null);
    when(penaltyRepository.findById(10L)).thenReturn(Optional.of(penalty));

    // when
    reactionGameService.submitResult(sessionId, "user-slow", 200.8);

    // then
    verify(reactionResultRepo).save(any(ReactionResult.class));

    org.mockito.ArgumentCaptor<su.kdt.minigame.domain.GamePenalty> penaltyCaptor =
            org.mockito.ArgumentCaptor.forClass(su.kdt.minigame.domain.GamePenalty.class);
    verify(gamePenaltyRepository).save(penaltyCaptor.capture());
    assertThat(penaltyCaptor.getValue().getUserUid()).isEqualTo("user-slow");

    verify(session).finish("패배 벌칙");

    @SuppressWarnings("unchecked")
    org.mockito.ArgumentCaptor<java.util.Map<String, Object>> payloadCaptor =
            (org.mockito.ArgumentCaptor<java.util.Map<String, Object>>) (org.mockito.ArgumentCaptor<?>) org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
    verify(messagingTemplate, times(2)).convertAndSend(anyString(), payloadCaptor.capture());
    var finalPayload = payloadCaptor.getAllValues().stream()
            .filter(p -> "GAME_FINISHED".equals(p.get("type"))).findFirst().orElseThrow();
    assertThat(finalPayload.get("loserUid")).isEqualTo("user-slow");
}

    @Test
    @DisplayName("결과 제출 시 아직 모든 참여자가 제출하지 않았다면 벌칙 배정은 하지 않는다")
    void submitResultWithoutAssignPenalty() {
        // given
        Long sessionId = 1L;
        GameSession session = mock(GameSession.class);
        when(session.getGameType()).thenReturn(GameSession.GameType.REACTION); // ✅ 핵심 추가
        when(gameRepo.findById(sessionId)).thenReturn(Optional.of(session));

        when(reactionResultRepo.save(any(ReactionResult.class))).thenAnswer(inv -> inv.getArgument(0));

        ReactionResult resultFast = new ReactionResult(session, "user-fast", 100.5);
        when(reactionResultRepo.findBySession(session)).thenReturn(List.of(resultFast)); // 끝까지 1명만

        // when
        reactionGameService.submitResult(sessionId, "user-slow", 120.0);

        // then
        verify(reactionResultRepo).save(any(ReactionResult.class));
        verify(gamePenaltyRepository, never()).save(any());
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(Object.class));
    }
}
