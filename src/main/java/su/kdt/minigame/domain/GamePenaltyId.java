package su.kdt.minigame.domain;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@EqualsAndHashCode
public class GamePenaltyId implements Serializable {

    private Long gameId; // GameSession 참조 [cite: 64]
    private String userUid; // User 참조 [cite: 64]

    public GamePenaltyId(Long gameId, String userUid) {
        this.gameId = gameId;
        this.userUid = userUid;
    }
}