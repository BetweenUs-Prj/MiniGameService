package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "quiz_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Setter를 제거하고 JPA를 위한 생성자 추가
public class QuizAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id") // ERD에 명시된 컬럼명과 일치
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private QuizRound round;

    @Column(name = "user_uid", nullable = false) // MSA를 위해 'user_id' -> 'user_uid' (String)으로 변경
    private String userUid;

    @Lob
    @Column(name = "answer_text", nullable = false)
    private String answerText;

    @Column(name = "answer_time", nullable = false)
    private Instant answerTime;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "response_time_ms") // 벌칙 계산을 위한 응답 시간 필드 추가
    private Long responseTimeMs;

    // 서비스 로직에서 사용할 생성자
    public QuizAnswer(QuizRound round, String userUid, String answerText) {
        this.round = round;
        this.userUid = userUid;
        this.answerText = answerText;
        this.answerTime = Instant.now();
    }

    // 정답 여부와 응답 시간을 기록하는 메소드
    public void grade(boolean isCorrect, long responseTimeMs) {
        this.isCorrect = isCorrect;
        if (isCorrect) {
            this.responseTimeMs = responseTimeMs;
        }
    }
}