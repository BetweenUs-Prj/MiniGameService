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
@Table(name = "reaction_result")
public class ReactionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "user_uid", nullable = false, length = 100)
    private String userUid;

    @Column(name = "delta_ms")
    private Integer deltaMs;

    @Column(name = "rank_order")
    private Integer rankOrder;

    @Column(name = "clicked_at")
    private Instant clickedAt;

    @Column(name = "false_start")
    private Boolean falseStart = false;

    public ReactionResult(Long roundId, String userUid) {
        this.roundId = roundId;
        this.userUid = userUid;
        this.falseStart = false;
        this.deltaMs = null;
    }

    public void recordClick(Instant clickTime, Integer deltaMs, boolean isFalseStart) {
        this.clickedAt = clickTime;
        this.falseStart = isFalseStart;
        this.deltaMs = isFalseStart ? null : deltaMs;
    }

    public void setRank(Integer rank) {
        this.rankOrder = rank;
    }

    // Compatibility methods and getters
    public Long getRoundId() {
        return this.roundId;
    }

    public Integer getDeltaMs() {
        return this.deltaMs;
    }

    public Boolean getFalseStart() {
        return this.falseStart;
    }

    public Integer getRankOrder() {
        return this.rankOrder;
    }

    public Long getResultId() {
        return this.resultId;
    }

    public Instant getClickedAt() {
        return this.clickedAt;
    }
    
    public String getUserUid() {
        return this.userUid;
    }
    
    // For backward compatibility with old session-based methods
    public Long getSessionId() {
        return this.roundId; // Round ID is now the primary identifier
    }
}