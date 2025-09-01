package su.kdt.minigame.dto;

import su.kdt.minigame.domain.Penalty;

public record PenaltyDto(
        Long id,
        String text
) {
    public static PenaltyDto from(Penalty penalty) {
        return new PenaltyDto(
                penalty.getPenaltyId(),
                penalty.getText()
        );
    }
}