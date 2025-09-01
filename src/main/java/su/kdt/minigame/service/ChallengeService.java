package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su.kdt.minigame.domain.ChallengeEntry;
import su.kdt.minigame.domain.ChallengeGroup;
import su.kdt.minigame.dto.response.LeaderboardDto;
import su.kdt.minigame.dto.response.PresenceDto;
import su.kdt.minigame.repository.ChallengeEntryRepository;
import su.kdt.minigame.repository.ChallengeGroupRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeService {

    private final ChallengeGroupRepository groupRepository;
    private final ChallengeEntryRepository entryRepository;

    /**
     * 그룹 리더보드 조회 (완료된 참가자만)
     */
    public LeaderboardDto getGroupLeaderboard(Long groupId, Pageable pageable) {
        Page<ChallengeEntry> entries = entryRepository.findLeaderboard(groupId, pageable);
        
        List<LeaderboardDto.LeaderboardEntry> leaderboardEntries = IntStream.range(0, entries.getContent().size())
            .mapToObj(i -> {
                ChallengeEntry entry = entries.getContent().get(i);
                int rank = (int) pageable.getOffset() + i + 1;
                
                return LeaderboardDto.LeaderboardEntry.builder()
                    .userUid(entry.getUserUid())
                    .displayName(entry.getUserUid().substring(0, Math.min(8, entry.getUserUid().length())))
                    .totalScore(entry.getTotalScore())
                    .correctCount(entry.getCorrectCount())
                    .totalQuestions(entry.getTotalQuestions())
                    .durationMs(entry.getDurationMs())
                    .rank(rank)
                    .finishedAt(entry.getFinishedAt())
                    .isCurrentUser(false) // 클라이언트에서 userUid 비교로 설정
                    .build();
            })
            .toList();
        
        return LeaderboardDto.builder()
            .groupId(groupId)
            .entries(leaderboardEntries)
            .totalEntries((int) entries.getTotalElements())
            .currentPage(entries.getNumber())
            .totalPages(entries.getTotalPages())
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 현재 활동 중인 참가자 조회 (30초 이내)
     */
    public PresenceDto getLiveParticipants(Long groupId) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusSeconds(30);
        List<ChallengeEntry> activeEntries = entryRepository.findActiveParticipants(groupId, cutoffTime);
        
        List<PresenceDto.ActiveParticipant> participants = activeEntries.stream()
            .map(entry -> PresenceDto.ActiveParticipant.builder()
                .userUid(entry.getUserUid())
                .displayName(entry.getUserUid().substring(0, Math.min(8, entry.getUserUid().length())))
                .status(entry.getStatus().name())
                .lastSeenAt(entry.getLastSeenAt())
                .isCurrentUser(false) // 클라이언트에서 userUid 비교로 설정
                .build())
            .toList();
        
        long totalParticipants = entryRepository.countByGroupId(groupId);
        
        return PresenceDto.builder()
            .groupId(groupId)
            .activeParticipants(participants)
            .totalParticipants((int) totalParticipants)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * Heartbeat 업데이트 (last_seen_at 갱신)
     */
    @Transactional
    public void updateHeartbeat(Long sessionId, String userUid, Map<String, Object> heartbeat) {
        // 세션 ID로부터 groupId 추출 (임시로 sessionId = groupId로 가정)
        // 실제로는 GameSession -> ChallengeEntry 매핑 필요
        Long groupId = sessionId; // TODO: 실제 매핑 로직
        
        entryRepository.updateLastSeen(groupId, userUid, LocalDateTime.now());
        
        log.debug("[HEARTBEAT-UPDATED] gid={}, uid={}, hb={}", groupId, userUid, heartbeat);
    }

    /**
     * 그룹 생성
     */
    @Transactional
    public ChallengeGroup createGroup(Long quizId, String createdBy, String title, String category) {
        ChallengeGroup group = new ChallengeGroup(quizId, createdBy, title, category);
        return groupRepository.save(group);
    }

    /**
     * 그룹 참가 (UNIQUE 제약으로 중복 방지)
     */
    @Transactional
    public ChallengeEntry joinGroup(Long groupId, String userUid) {
        // 이미 참가했는지 확인
        return entryRepository.findByGroupIdAndUserUid(groupId, userUid)
            .orElseGet(() -> {
                ChallengeEntry entry = new ChallengeEntry(groupId, userUid);
                return entryRepository.save(entry);
            });
    }

    /**
     * 세션 완료 시 챌린지 엔트리 업데이트
     */
    @Transactional 
    public void finishSession(Long sessionId, String userUid, Integer totalScore, 
                             Integer correctCount, Integer totalQuestions, Long durationMs) {
        // sessionId -> groupId 매핑 (실제 구현 필요)
        Long groupId = sessionId; // TODO: 실제 매핑
        
        entryRepository.findByGroupIdAndUserUid(groupId, userUid)
            .ifPresent(entry -> {
                entry.finishSession(totalScore, correctCount, totalQuestions, durationMs);
                entryRepository.save(entry);
                
                // 순위 재계산
                updateRankings(groupId);
                
                log.info("[CHALLENGE-FINISHED] gid={}, uid={}, score={}, correct={}/{}", 
                    groupId, userUid, totalScore, correctCount, totalQuestions);
            });
    }

    /**
     * 그룹 내 순위 재계산
     */
    @Transactional
    public void updateRankings(Long groupId) {
        List<ChallengeEntry> entries = entryRepository.findAllFinishedByGroupId(groupId);
        
        for (int i = 0; i < entries.size(); i++) {
            ChallengeEntry entry = entries.get(i);
            int rank = i + 1;
            entryRepository.updateRankPosition(entry.getId(), rank);
        }
        
        log.debug("[RANKINGS-UPDATED] gid={}, entries={}", groupId, entries.size());
    }
}