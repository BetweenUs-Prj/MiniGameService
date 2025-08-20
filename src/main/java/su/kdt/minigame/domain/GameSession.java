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

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "host_uid", nullable = false) // 방장 ID 필드 추가
    private String hostUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false)
    private GameType gameType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.WAITING;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String penalty;
    
    @Column(name = "loser_uid")
    private String loserUid;

    // 생성자에 hostUid 파라미터 추가
    public GameSession(Long appointmentId, GameType gameType, String hostUid) {
        this.appointmentId = appointmentId;
        this.gameType = gameType;
        this.hostUid = hostUid;
    }

    public void start() {
        if (this.status == Status.WAITING) {
            this.status = Status.IN_PROGRESS;
            this.startTime = LocalDateTime.now();
        }
    }

    public void finishGame(String loserUid, String penalty) {
        if (this.status == Status.IN_PROGRESS) {
            this.status = Status.FINISHED;
            this.endTime = LocalDateTime.now();
            this.loserUid = loserUid;
            this.penalty = penalty;
        }
    }
}