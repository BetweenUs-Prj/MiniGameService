package su.kdt.minigame.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import su.kdt.minigame.domain.Penalty;
import su.kdt.minigame.repository.PenaltyRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PenaltyServiceTest {

    @Mock
    private PenaltyRepository penaltyRepository;

    @InjectMocks
    private PenaltyService penaltyService;

    private final String userUid = "test-user";

    @Test
    @DisplayName("사용자 정의 벌칙과 기본 벌칙 목록을 함께 조회한다")
    void getPenalties() {
        // given
        Penalty userPenalty = new Penalty("라면 사오기", userUid);
        Penalty defaultPenalty = new Penalty("커피 쏘기", null);

        // 불변 리스트(List.of) 대신 가변 리스트 반환 (서비스 내부에서 add/addAll 시 UnsupportedOperationException 방지)
        when(penaltyRepository.findByUserUid(userUid))
                .thenReturn(new ArrayList<>(List.of(userPenalty)));
        when(penaltyRepository.findByUserUidIsNull())
                .thenReturn(new ArrayList<>(List.of(defaultPenalty)));

        // when
        List<Penalty> penalties = penaltyService.getPenalties(userUid);

        // then
        assertThat(penalties).hasSize(2);
        assertThat(penalties).contains(userPenalty, defaultPenalty);
    }

    @Test
    @DisplayName("새로운 벌칙을 생성한다")
    void createPenalty() {
        // given
        String description = "새 벌칙";

        // when
        penaltyService.createPenalty(description, userUid);

        // then
        ArgumentCaptor<Penalty> captor = ArgumentCaptor.forClass(Penalty.class);
        verify(penaltyRepository).save(captor.capture());
        Penalty savedPenalty = captor.getValue();

        assertThat(savedPenalty.getDescription()).isEqualTo(description);
        assertThat(savedPenalty.getUserUid()).isEqualTo(userUid);
    }

    @Test
    @DisplayName("자신이 만든 벌칙을 성공적으로 수정한다")
    void updateMyPenalty() {
        // given
        Long penaltyId = 1L;
        String newDescription = "수정된 벌칙";

        // 실객체로 검증(목 사용 시 final/record 이슈 회피)
        Penalty myPenalty = new Penalty("원래 벌칙", userUid);
        when(penaltyRepository.findById(penaltyId)).thenReturn(Optional.of(myPenalty));

        // when
        penaltyService.updatePenalty(penaltyId, newDescription, userUid);

        // then
        assertThat(myPenalty.getDescription()).isEqualTo(newDescription);
    }

    @Test
    @DisplayName("다른 사람의 벌칙을 수정하려고 하면 예외가 발생한다")
    void updateOthersPenalty() {
        // given
        Long penaltyId = 1L;
        Penalty othersPenalty = new Penalty("남의 벌칙", "other-user");
        when(penaltyRepository.findById(penaltyId)).thenReturn(Optional.of(othersPenalty));

        // when & then
        assertThatThrownBy(() -> penaltyService.updatePenalty(penaltyId, "수정 시도", userUid))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("You can only update your own penalties");
    }

    @Test
    @DisplayName("자신이 만든 벌칙을 성공적으로 삭제한다")
    void deleteMyPenalty() {
        // given
        Long penaltyId = 1L;
        Penalty myPenalty = new Penalty("삭제할 벌칙", userUid);
        when(penaltyRepository.findById(penaltyId)).thenReturn(Optional.of(myPenalty));

        // when
        penaltyService.deletePenalty(penaltyId, userUid);

        // then
        verify(penaltyRepository).delete(myPenalty);
    }

    @Test
    @DisplayName("다른 사람의 벌칙을 삭제하려고 하면 예외가 발생한다")
    void deleteOthersPenalty() {
        // given
        Long penaltyId = 1L;
        Penalty othersPenalty = new Penalty("남의 벌칙", "other-user");
        when(penaltyRepository.findById(penaltyId)).thenReturn(Optional.of(othersPenalty));

        // when & then
        assertThatThrownBy(() -> penaltyService.deletePenalty(penaltyId, userUid))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("You can only delete your own penalties");
    }
}
