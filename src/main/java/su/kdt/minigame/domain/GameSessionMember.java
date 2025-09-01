package su.kdt.minigame.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_session_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(GameSessionMemberId.class)
public class GameSessionMember {

    @Id
    @Column(name = "session_id")
    private Long sessionId;

    @Id
    @Column(name = "user_uid")
    private String userUid;

    @Column(name = "is_ready")
    private Boolean isReady = false;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    public GameSessionMember(Long sessionId, String userUid) {
        this.sessionId = sessionId;
        this.userUid = userUid;
        this.isReady = false;
        this.joinedAt = LocalDateTime.now();
    }

    public void setReady(boolean ready) {
        this.isReady = ready;
    }

    public boolean isReady() {
        return Boolean.TRUE.equals(isReady);
    }

    // 임시로 nickname을 userUid로 반환 (실제로는 별도 필드가 필요할 수 있음)
    public String getNickname() {
        return this.userUid;
    }
}