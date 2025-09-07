package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "penalty", 
       uniqueConstraints = @UniqueConstraint(name = "ux_penalty_slug", columnNames = "slug"))
public class Penalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long penaltyId;

    @Column(name = "slug", nullable = false, unique = true, length = 64)
    private String slug;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "penalty_text", nullable = false, length = 255)
    private String text;

    @Column(name = "created_at")
    private Instant createdAt;

    public Penalty(String text, Long userId) {
        this.text = text;
        this.userId = userId;
        this.slug = generateSlug(text); // 🔥 핵심 수정: slug 자동 생성
        this.createdAt = Instant.now();
    }
    
    // 멱등한 시드용 생성자 (slug 기반) - 매개변수 타입 구분을 위해 slug 전용 생성자
    public Penalty(String slug, String text, String systemMarker) {
        this.slug = slug;
        this.text = text;
        this.userId = 0L; // 시스템 기본 벌칙 (user_id = 0)
        this.createdAt = Instant.now();
    }
    
    // slug 자동 생성 (한글 지원)
    public static String generateSlug(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "penalty-" + System.currentTimeMillis();
        }
        
        // 한글 텍스트는 timestamp 기반 slug로 변환
        String slug = text.toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9\\-가-힣]", "")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
                
        // 한글만 있거나 빈 문자열인 경우 timestamp 기반으로 생성
        if (slug.isEmpty() || slug.matches(".*[가-힣].*")) {
            return "penalty-" + System.currentTimeMillis();
        }
        
        return slug;
    }
    
    // setter 메소드들 추가
    public void setText(String text) {
        this.text = text;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    // 하위 호환성을 위한 메소드들
    public Long getId() {
        return this.penaltyId;
    }
    
    public Long getUserId() {
        return this.userId;
    }
    
    // Backward compatibility method
    public String getUserUid() {
        return String.valueOf(this.userId);
    }
    
    public String getDescription() {
        return this.text;
    }
    
    public void updateDescription(String newDescription) {
        this.text = newDescription;
    }
}