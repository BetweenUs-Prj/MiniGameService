package su.kdt.minigame.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.Instant;

@Getter @AllArgsConstructor
public class CreatePenaltyRes {
    private Long id;
    private String text;
    private String userUid;
    private Instant createdAt;
}