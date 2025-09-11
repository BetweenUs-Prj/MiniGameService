package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_round")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "round_id")
    private Long roundId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public QuizRound(Long sessionId, Integer roundNo, QuizQuestion question) {
        this.sessionId = sessionId;
        this.roundNo = roundNo;
        this.question = question;
        this.startsAt = LocalDateTime.now();
        this.expiresAt = this.startsAt.plusSeconds(30); // 30초 제한
    }

    // 시작/만료 시간을 직접 지정하고 싶을 때 쓰는 생성자
    public QuizRound(Long sessionId, Integer roundNo, QuizQuestion question,
                     LocalDateTime startsAt, LocalDateTime expiresAt) {
        this.sessionId = sessionId;
        this.roundNo = roundNo;
        this.question = question;
        this.startsAt = startsAt;
        this.expiresAt = expiresAt;
    }

    // Legacy constructor for backward compatibility
    public QuizRound(Long sessionId, QuizQuestion question) {
        this(sessionId, 1, question);
    }

    public LocalDateTime getStartTime() {
        return this.startsAt;
    }

    public void endRound() {
        this.endedAt = LocalDateTime.now();
    }

}
