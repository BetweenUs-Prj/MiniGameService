package su.kdt.minigame.domain;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter; // ◀◀◀ import 추가
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@EqualsAndHashCode
@Getter // ◀◀◀ 이 어노테이션을 추가해주세요!
public class GamePenaltyId implements Serializable {

    private Long gameId; // GameSession 참조
    private Long userId; // User 참조 (BIGINT 타입)

    public GamePenaltyId(Long gameId, Long userId) {
        this.gameId = gameId;
        this.userId = userId;
    }
}