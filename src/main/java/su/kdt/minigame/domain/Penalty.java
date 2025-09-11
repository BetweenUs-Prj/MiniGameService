package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "penalty",
    uniqueConstraints = @UniqueConstraint(name = "ux_penalty_slug", columnNames = "slug")
)
public class Penalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "penalty_id")          // ✅ DB와 일치하게 수정
    private Long penaltyId;

    @Column(name = "slug", nullable = false, length = 64) // ← unique=true 제거 (중복 인덱스 방지)
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
        this.slug = generateSlug(text);   // 기본 slug 생성
        this.createdAt = Instant.now();
    }

    // 멱등 시드용 (slug 지정)
    public Penalty(String slug, String text, String systemMarker) {
        this.slug = slug;
        this.text = text;
        this.userId = 0L;
        this.createdAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (this.slug == null || this.slug.isBlank()) {
            this.slug = generateSlug(this.text);
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // 한글 포함 시 타임스탬프 slug로 폴백
    public static String generateSlug(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "penalty-" + System.currentTimeMillis();
        }
        String slug = text.toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9\\-가-힣]", "")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (slug.isEmpty() || slug.matches(".*[가-힣].*")) {
            return "penalty-" + System.currentTimeMillis();
        }
        return slug;
    }

    // 선택적 세터 (필요 시 유지)
    public void setText(String text) { this.text = text; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // 하위 호환
    public Long getId() { return this.penaltyId; }
    public String getUserUid() { return String.valueOf(this.userId); }
    public String getDescription() { return this.text; }
    public void updateDescription(String newDescription) { this.text = newDescription; }
}
