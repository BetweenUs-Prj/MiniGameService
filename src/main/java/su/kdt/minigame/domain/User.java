package su.kdt.minigame.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    private String uid; // 사용자 식별자

    @Column(nullable = false)
    private String username; // 사용자명

    @Column(name = "kakao_id", nullable = false, unique = true)
    private Long kakaoId; // 카카오 식별자

    private String profile; // 프로필 사진 URL

    private String address; // 기본 출발지

    @Column(name = "prefer_transport")
    private String preferTransport; // 선호하는 교통수단

    @Column(name = "prefer_criteria")
    private String preferCriteria; // 선호하는 거리산정 기준
}