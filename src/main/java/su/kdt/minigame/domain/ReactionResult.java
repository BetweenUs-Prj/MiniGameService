package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reaction_result")
@Getter
@Setter
public class ReactionResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 게임 세션에 속한 결과인지 연결합니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 반응 속도를 ms 단위로 저장합니다. (예: 128.5ms)
    @Column(name = "reaction_time", nullable = false)
    private double reactionTime;

    // 게임 종료 후 순위를 기록합니다.
    @Column(name = "ranking")
    private int ranking;
}