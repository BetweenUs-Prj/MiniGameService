package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reaction_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Setter를 제거하고, JPA를 위한 기본 생성자 추가
public class ReactionResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @Column(name = "user_uid", nullable = false) // MSA를 위해 'user_id' -> 'user_uid' (String)으로 변경
    private String userUid;

    @Column(name = "reaction_time", nullable = false)
    private double reactionTime;

    @Column(name = "ranking")
    private int ranking;

    // 필수 데이터를 받는 생성자 추가
    public ReactionResult(GameSession session, String userUid, double reactionTime) {
        this.session = session;
        this.userUid = userUid;
        this.reactionTime = reactionTime;
    }

    // 랭킹을 업데이트하는 특정 메소드 제공
    public void updateRanking(int ranking) {
        this.ranking = ranking;
    }
}