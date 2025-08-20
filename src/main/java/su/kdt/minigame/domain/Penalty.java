package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "penalty")
public class Penalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description")
    private String description;

    // ===== 이 필드를 추가합니다! =====
    @Column(name = "user_uid")
    private String userUid; // 이 벌칙을 생성한 사용자의 ID

    public Penalty(String description, String userUid) {
        this.description = description;
        this.userUid = userUid;
    }
}