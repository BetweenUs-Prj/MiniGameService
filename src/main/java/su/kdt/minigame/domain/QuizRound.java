package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_round")
@Getter
@Setter
public class QuizRound {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "winner_user_id")
    private Long winnerUserId;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public boolean isClosed() {
        return this.endTime != null;
    }

    /**
     * 승자를 결정하는 로직을 엔티티가 직접 처리합니다.
     * 이미 승자가 결정되었다면 아무것도 하지 않습니다.
     * @param userId 새로운 승자 후보의 ID
     */
    public void decideWinner(Long userId) {
        if (!isClosed()) {
            this.winnerUserId = userId;
            this.endTime = LocalDateTime.now();
        }
    }
}