package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Challenge Group: 같은 퀴즈 세트를 공유하는 그룹
 * 각 사용자는 개별 세션으로 참여하지만 같은 그룹 내에서 리더보드 경쟁
 */
@Entity
@Table(name = "challenge_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeGroup {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;
    
    @Column(name = "quiz_id", nullable = false)
    private Long quizId;
    
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    
    @Column(name = "title", length = 200)
    private String title;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "category", length = 50)
    private String category;
    
    @Column(name = "max_participants")
    private Integer maxParticipants;
    
    @Column(name = "is_active", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "ends_at")
    private LocalDateTime endsAt;
    
    public ChallengeGroup(Long quizId, String createdBy, String title, String category) {
        this.quizId = quizId;
        this.createdBy = createdBy;
        this.title = title;
        this.category = category;
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
    
    public void setEndsAt(LocalDateTime endsAt) {
        this.endsAt = endsAt;
    }
    
    public void deactivate() {
        this.isActive = false;
    }
}