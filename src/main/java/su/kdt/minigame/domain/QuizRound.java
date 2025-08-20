package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_round")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Setter를 제거하고, JPA를 위한 생성자 추가
public class QuizRound {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "round_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    // QuizService에서 사용할 생성자
    public QuizRound(GameSession session, QuizQuestion question) {
        this.session = session;
        this.question = question;
        this.startTime = LocalDateTime.now();
    }
}