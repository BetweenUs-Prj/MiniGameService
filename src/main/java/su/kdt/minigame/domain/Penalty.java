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

    @Column(name = "user_uid")
    private String userUid;

    public Penalty(String description, String userUid) {
        this.description = description;
        this.userUid = userUid;
    }
    
    // 내용을 업데이트하는 메소드 추가
    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }
}