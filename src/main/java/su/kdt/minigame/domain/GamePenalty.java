package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "game_penalty")
public class GamePenalty {

    @EmbeddedId
    private GamePenaltyId id;

    @MapsId("gameId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private GameSession gameSession;
    
    // ◀◀◀ 이전에 있었던 중복 필드를 완전히 삭제했습니다.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penalty_id", nullable = false)
    private Penalty penalty;

    public GamePenalty(GameSession gameSession, String userUid, Penalty penalty) {
        this.id = new GamePenaltyId(gameSession.getId(), userUid);
        this.gameSession = gameSession;
        this.penalty = penalty;
    }
    
    // 서비스 계층에서 userUid에 쉽게 접근할 수 있도록 Getter를 추가합니다.
    public String getUserUid() {
        return this.id.getUserUid();
    }
}