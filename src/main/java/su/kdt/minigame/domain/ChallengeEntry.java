package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Challenge Entry: 그룹 참가자의 게임 결과
 * UNIQUE (group_id, user_uid)로 완료 시 upsert
 */
@Entity
@Table(name = "challenge_entry", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_uid"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeEntry {
    
    public enum Status { JOINED, IN_PROGRESS, FINISHED }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_id")
    private Long id;
    
    @Column(name = "group_id", nullable = false)
    private Long groupId;
    
    @Column(name = "user_uid", nullable = false, length = 100)
    private String userUid;
    
    @Column(name = "session_id")
    private Long sessionId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.JOINED;
    
    @Column(name = "total_score")
    private Integer totalScore = 0;
    
    @Column(name = "correct_count")
    private Integer correctCount = 0;
    
    @Column(name = "total_questions")
    private Integer totalQuestions = 0;
    
    @Column(name = "duration_ms")
    private Long durationMs;
    
    @Column(name = "rank_position")
    private Integer rankPosition;
    
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
    
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;
    
    public ChallengeEntry(Long groupId, String userUid) {
        this.groupId = groupId;
        this.userUid = userUid;
        this.status = Status.JOINED;
        this.joinedAt = LocalDateTime.now();
        this.lastSeenAt = LocalDateTime.now();
    }
    
    public void startSession(Long sessionId) {
        this.sessionId = sessionId;
        this.status = Status.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
        this.lastSeenAt = LocalDateTime.now();
    }
    
    public void finishSession(Integer totalScore, Integer correctCount, Integer totalQuestions, Long durationMs) {
        this.status = Status.FINISHED;
        this.totalScore = totalScore;
        this.correctCount = correctCount;
        this.totalQuestions = totalQuestions;
        this.durationMs = durationMs;
        this.finishedAt = LocalDateTime.now();
        this.lastSeenAt = LocalDateTime.now();
    }
    
    public void updateLastSeen() {
        this.lastSeenAt = LocalDateTime.now();
    }
    
    public void updateRankPosition(Integer rankPosition) {
        this.rankPosition = rankPosition;
    }
    
    /**
     * 30초 이내 활동 여부 체크 (presence용)
     */
    public boolean isActiveWithin30Seconds() {
        return lastSeenAt != null && 
               lastSeenAt.isAfter(LocalDateTime.now().minusSeconds(30));
    }
}