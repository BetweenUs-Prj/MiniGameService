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

    // ===== Status Enum 추가 =====
    public enum Status {
        IN_PROGRESS, // 진행중
        COMPLETED    // 모든 참여자가 답변을 제출하여 완료됨
    }

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

    // ===== status 필드 추가 =====
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.IN_PROGRESS;

    public QuizRound(GameSession session, QuizQuestion question) {
        this.session = session;
        this.question = question;
        this.startTime = LocalDateTime.now();
    }

    // ===== 라운드를 완료 상태로 바꾸는 메소드 추가 =====
    public void complete() {
        if (this.status == Status.IN_PROGRESS) {
            this.status = Status.COMPLETED;
        }
    }
}
