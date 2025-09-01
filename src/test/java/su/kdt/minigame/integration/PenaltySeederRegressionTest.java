package su.kdt.minigame.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import su.kdt.minigame.config.PenaltySeeder;
import su.kdt.minigame.repository.PenaltyRepository;

import static org.assertj.core.api.Assertions.*;

/**
 * 벌칙 중복 생성 버그 회귀 테스트
 * 
 * 수정 전 문제:
 * - spring.sql.init.mode: always + data.sql로 인한 재시작시 중복 생성
 * - 기본 벌칙 5개가 재시작마다 계속 추가됨
 * 
 * 수정 후 검증:
 * - PenaltySeeder의 멱등성으로 중복 생성 방지
 * - slug 기반 UNIQUE 제약으로 DB 레벨 중복 차단
 * - 3회 연속 시드 실행해도 총 개수 불변
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PenaltySeederRegressionTest {

    @Autowired
    private PenaltyRepository penaltyRepository;
    
    @Autowired
    private PenaltySeeder penaltySeeder;

    @Test
    @DisplayName("멱등성 검증: 3회 연속 시드 실행해도 기본 벌칙 개수 불변")
    public void testPenaltySeedingIsIdempotent() throws Exception {
        // Given: 초기 상태 확인
        long initialCount = penaltyRepository.count();
        
        // When: 시드를 3회 연속 실행 (백엔드 재시작 시뮬레이션)
        penaltySeeder.run();
        long countAfterFirst = penaltyRepository.count();
        
        penaltySeeder.run();  
        long countAfterSecond = penaltyRepository.count();
        
        penaltySeeder.run();
        long countAfterThird = penaltyRepository.count();
        
        // Then: 개수가 변하지 않아야 함 (멱등성)
        assertThat(countAfterFirst).isEqualTo(countAfterSecond)
            .withFailMessage("두 번째 시드 실행 후 개수가 증가했습니다 (중복 생성 버그)");
            
        assertThat(countAfterSecond).isEqualTo(countAfterThird)
            .withFailMessage("세 번째 시드 실행 후 개수가 증가했습니다 (중복 생성 버그)");
            
        // 최소한 기본 벌칙 5개는 있어야 함
        assertThat(countAfterThird).isGreaterThanOrEqualTo(5)
            .withFailMessage("기본 벌칙이 제대로 생성되지 않았습니다");
    }
    
    @Test
    @DisplayName("UNIQUE 제약 검증: slug 중복 시 데이터베이스 레벨에서 차단")
    public void testSlugUniquenessConstraint() {
        // Given: 기본 시드 실행
        assertThatNoException().isThrownBy(() -> penaltySeeder.run());
        
        // When & Then: slug가 같은 벌칙을 직접 저장하면 제약 위반
        assertThatThrownBy(() -> {
            penaltyRepository.saveAndFlush(
                new su.kdt.minigame.domain.Penalty("buy-coffee", "중복 테스트", "system")
            );
        }).hasMessageContaining("ux_penalty_slug")
          .withFailMessage("UNIQUE 제약이 작동하지 않습니다");
    }
    
    @Test
    @DisplayName("기본 벌칙 존재 확인: 필수 벌칙들이 모두 생성되는지 검증")
    public void testDefaultPenaltiesAreCreated() throws Exception {
        // Given & When: 시드 실행
        penaltySeeder.run();
        
        // Then: 기본 벌칙들이 모두 존재해야 함
        String[] expectedSlugs = {
            "buy-coffee", "buy-ice-cream", "buy-chicken", 
            "sing-song", "dance-song"
        };
        
        for (String slug : expectedSlugs) {
            assertThat(penaltyRepository.findBySlug(slug))
                .isPresent()
                .withFailMessage("기본 벌칙 '%s'가 생성되지 않았습니다", slug);
        }
        
        // 모든 기본 벌칙의 userUid는 "system"이어야 함
        for (String slug : expectedSlugs) {
            var penalty = penaltyRepository.findBySlug(slug).orElseThrow();
            assertThat(penalty.getUserUid()).isEqualTo("system")
                .withFailMessage("기본 벌칙 '%s'의 userUid가 'system'이 아닙니다", slug);
        }
    }
}