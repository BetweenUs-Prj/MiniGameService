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

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "delta_ms")
    private Integer deltaMs;

    @Column(name = "rank_order")
    private Integer rankOrder;

    @Column(name = "clicked_at")
    private Instant clickedAt;

    @Column(name = "false_start")
    private Boolean falseStart = false;

    public ReactionResult(Long sessionId, Long userId) {
        this.sessionId = sessionId;
        this.userId = userId;
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
        return this.sessionId; // For backward compatibility
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
    
    public Long getUserId() {
        return this.userId;
    }
    
    // Backward compatibility method
    public String getUserUid() {
        return String.valueOf(this.userId);
    }
    
    // For backward compatibility with old session-based methods
    public Long getSessionId() {
        return this.sessionId;
    }
}