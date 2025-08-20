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
    private GamePenaltyId id; // 복합 키 사용

    @MapsId("gameId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private GameSession gameSession;

    // 🔴 이 부분을 수정합니다!
    // @MapsId("userUid")
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "user_uid")
    // private User user; // User 객체 직접 참조 대신,
    
    @Column(name = "user_uid", insertable=false, updatable=false) // ID 클래스와의 매핑을 위해 추가
    private String userUid; // user의 uid만 String으로 저장

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penalty_id", nullable = false)
    private Penalty penalty;

    // 생성자도 uid를 직접 받도록 수정
    public GamePenalty(GameSession gameSession, String userUid, Penalty penalty) {
        this.id = new GamePenaltyId(gameSession.getId(), userUid);
        this.gameSession = gameSession;
        this.userUid = userUid;
        this.penalty = penalty;
    }
}