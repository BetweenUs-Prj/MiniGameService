package su.kdt.minigame.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import su.kdt.minigame.domain.Penalty;
import su.kdt.minigame.repository.PenaltyRepository;

/**
 * 멱등한 기본 벌칙 시드 
 * data.sql 대신 slug 기반으로 중복 없이 기본 벌칙 생성
 */
@Slf4j
@Component
@Order(1000) // Flyway 마이그레이션 이후 실행
@RequiredArgsConstructor
public class PenaltySeeder implements CommandLineRunner {

    private final PenaltyRepository penaltyRepository;

    // 기본 벌칙 정의 (slug → text 매핑)
    private static final String[][] DEFAULT_PENALTIES = {
        {"buy-coffee", "커피 한 잔 사기"},
        {"buy-ice-cream", "아이스크림 사기"},
        {"buy-chicken", "치킨 한 마리 사기"},
        {"sing-song", "노래 한 곡 부르기"},
        {"dance-song", "댄스 한 곡 추기"}
    };

    @Override
    public void run(String... args) throws Exception {
        log.info("🎯 Starting idempotent penalty seeding...");
        
        // 1단계: 잘못된 벌칙들 정리 (빈 slug나 null slug)
        cleanupInvalidPenalties();
        
        // 2단계: 중복된 기본 벌칙들 정리
        cleanupDuplicatePenalties();
        
        // 3단계: 누락된 기본 벌칙들 추가
        int created = 0;
        int skipped = 0;
        
        for (String[] penaltyData : DEFAULT_PENALTIES) {
            String slug = penaltyData[0];
            String text = penaltyData[1];
            
            // slug 기반으로 존재 여부 확인 (멱등)
            var existingPenalties = penaltyRepository.findBySlug(slug);
            if (existingPenalties.isEmpty()) {
                Penalty penalty = new Penalty(slug, text, "system");
                penaltyRepository.save(penalty);
                log.debug("✅ Created penalty: {} → {}", slug, text);
                created++;
            } else {
                log.debug("⏭️  Skipped existing penalty: {}", slug);
                skipped++;
            }
        }
        
        long totalPenalties = penaltyRepository.count();
        log.info("🏁 Penalty seeding completed: {} created, {} skipped, {} total penalties in DB", 
                created, skipped, totalPenalties);
    }
    
    private void cleanupInvalidPenalties() {
        log.debug("🧹 Cleaning up invalid penalties (empty or null slug)...");
        var invalidPenalties = penaltyRepository.findAll().stream()
            .filter(p -> p.getSlug() == null || p.getSlug().trim().isEmpty())
            .toList();
            
        if (!invalidPenalties.isEmpty()) {
            penaltyRepository.deleteAll(invalidPenalties);
            log.info("🗑️  Deleted {} invalid penalties with empty/null slug", invalidPenalties.size());
        }
    }
    
    private void cleanupDuplicatePenalties() {
        log.debug("🔍 Checking for duplicate penalties...");
        
        for (String[] penaltyData : DEFAULT_PENALTIES) {
            String slug = penaltyData[0];
            var existingPenalty = penaltyRepository.findBySlug(slug);
            
            if (existingPenalty.isPresent()) {
                log.debug("✅ Penalty with slug '{}' already exists", slug);
            } else {
                log.debug("ℹ️  No duplicate found for slug: {}", slug);
            }
        }
    }
}