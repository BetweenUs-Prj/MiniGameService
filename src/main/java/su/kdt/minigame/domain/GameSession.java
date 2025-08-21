package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameSession {

    public enum GameType { REACTION, QUIZ }
    public enum Status { WAITING, IN_PROGRESS, FINISHED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long id;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "host_uid", nullable = false)
    private String hostUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false)
    private GameType gameType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.WAITING;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Column(name = "selected_penalty_id")
    private Long selectedPenaltyId;

    @Column(name = "penalty_description")
    private String penaltyDescription;
    
    @Column(name = "total_rounds")
    private Integer totalRounds; // ◀◀◀ 퀴즈 문항 수 필드 추가

    // 생성자에서 totalRounds를 받도록 수정
    public GameSession(Long appointmentId, GameType gameType, String hostUid, Long selectedPenaltyId, Integer totalRounds) {
        this.appointmentId = appointmentId;
        this.gameType = gameType;
        this.hostUid = hostUid;
        this.selectedPenaltyId = selectedPenaltyId;
        this.totalRounds = totalRounds;
    }

    public void start() {
        if (this.status == Status.WAITING) {
            this.status = Status.IN_PROGRESS;
            this.startTime = LocalDateTime.now();
        }
    }
    
    public void finish(String penaltyDescription) {
        if (this.status == Status.IN_PROGRESS || this.status == Status.WAITING) {
            this.status = Status.FINISHED;
            this.endTime = LocalDateTime.now();
            this.penaltyDescription = penaltyDescription;
        }
    }
}
