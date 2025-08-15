package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_session")
@Getter
@Setter
public class GameSession {

    public enum GameType { REACTION, QUIZ }
    public enum Status { WAITING, IN_PROGRESS, FINISHED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long id;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false)
    private GameType gameType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.WAITING;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // ===== 아래 생성자를 새로 추가해주세요! =====
    
    // JPA가 데이터베이스에서 객체를 생성할 때 사용하는 기본 생성자
    public GameSession() {
    }

    // ReactionGameService에서 새 세션을 만들 때 사용하는 생성자
    public GameSession(Long appointmentId, GameType gameType) {
        this.appointmentId = appointmentId;
        this.gameType = gameType;
    }
    // ===== 여기까지 추가 =====

    public void start() {
        if (this.status == Status.WAITING) {
            this.status = Status.IN_PROGRESS;
            this.startTime = LocalDateTime.now();
        }
    }
}