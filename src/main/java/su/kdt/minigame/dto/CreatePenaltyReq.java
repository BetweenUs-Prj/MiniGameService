package su.kdt.minigame.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreatePenaltyReq {
    @NotBlank
    private String text;
    private String gameType;
    private Long sessionId;
}