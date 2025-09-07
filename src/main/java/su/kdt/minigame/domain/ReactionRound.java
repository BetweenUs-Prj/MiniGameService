package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reaction_round")
public class ReactionRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "round_id")
    private Long roundId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "WAITING";

    @Column(name = "red_at")
    private Instant redAt;

    @Column(name = "created_at")
    private Instant createdAt;

    public ReactionRound(Long sessionId) {
        this.sessionId = sessionId;
        this.status = "WAITING";
        this.createdAt = Instant.now();
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    public void setRedSignal() {
        this.status = "RED";
        this.redAt = Instant.now();
    }

    public void finish() {
        this.status = "FINISHED";
    }
}