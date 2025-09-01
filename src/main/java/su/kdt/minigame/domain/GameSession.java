package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.Instant;

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
    @Setter
    private Status status = Status.WAITING;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    @Setter
    private Instant startedAt;

    @Column(name = "selected_penalty_id")
    private Long selectedPenaltyId;

    @Column(name = "penalty_description")
    private String penaltyDescription;
    
    @Column(name = "total_rounds", columnDefinition = "INT DEFAULT 5")
    private Integer totalRounds;

    @Column(name = "current_round", columnDefinition = "INT DEFAULT 0")
    private Integer currentRound = 0;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "is_private", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isPrivate = false;

    @Column(name = "pin_hash")
    private String pinHash;

    // 생성자에서 totalRounds와 category를 받도록 수정
    public GameSession(Long appointmentId, GameType gameType, String hostUid, Long selectedPenaltyId, String penaltyText, Integer totalRounds, String category) {
        this.appointmentId = appointmentId;
        this.gameType = gameType;
        this.hostUid = hostUid;
        this.selectedPenaltyId = selectedPenaltyId;
        this.penaltyDescription = penaltyText;
        this.totalRounds = totalRounds;
        this.category = category;
        this.currentRound = 0;
    }

    public void setIsPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
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

    public void close() {
        if (this.status == Status.WAITING) {
            this.status = Status.CANCELLED;
            this.endTime = LocalDateTime.now();
        }
    }
    
    public int incAndGetCurrentRound() {
        this.currentRound++;
        return this.currentRound;
    }
    
    public String getCategoryEnum() {
        return this.category;
    }
}
