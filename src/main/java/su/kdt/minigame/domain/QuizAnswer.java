package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "quiz_answer", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"round_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id") // ERD에 명시된 컬럼명과 일치
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private QuizRound round;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "choice_index", nullable = false)
    private Integer choiceIndex;

    @Lob
    @Column(name = "answer_text")
    private String answerText;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "score", columnDefinition = "INT DEFAULT 0")
    private Integer score = 0;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    // 새로운 생성자 (choice_index 포함)
    public QuizAnswer(QuizRound round, Long userId, Integer choiceIndex) {
        this.round = round;
        this.userId = userId;
        this.choiceIndex = choiceIndex;
        this.answerText = String.valueOf(choiceIndex); // 호환성을 위해 유지
        this.submittedAt = Instant.now();
    }

    // Legacy constructor for backward compatibility  
    public QuizAnswer(QuizRound round, Long userId, String answerText) {
        this.round = round;
        this.userId = userId;
        this.answerText = answerText;
        this.submittedAt = Instant.now();
        try {
            this.choiceIndex = Integer.parseInt(answerText);
        } catch (NumberFormatException e) {
            this.choiceIndex = 0;
        }
    }

    // 점수 부여 메서드
    public void grade(boolean isCorrect, int score) {
        this.isCorrect = isCorrect;
        this.score = score;
    }

    // Legacy method for backward compatibility
    public void grade(boolean isCorrect, long responseTimeMs) {
        this.isCorrect = isCorrect;
        this.score = isCorrect ? 10 : 0;
        this.responseTimeMs = responseTimeMs;
    }

    public Instant getAnswerTime() {
        return this.submittedAt;
    }
}