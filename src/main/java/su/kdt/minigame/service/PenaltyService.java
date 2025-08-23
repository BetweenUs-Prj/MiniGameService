package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su.kdt.minigame.domain.Penalty;
import su.kdt.minigame.repository.PenaltyRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PenaltyService {

    private final PenaltyRepository penaltyRepository;

    /**
     * 특정 사용자가 볼 수 있는 모든 벌칙 목록을 조회합니다.
     * (사용자 정의 벌칙 + 기본 벌칙)
     */
    @Transactional(readOnly = true)
    public List<Penalty> getPenalties(String userUid) {
        List<Penalty> userPenalties = penaltyRepository.findByUserUid(userUid);
        List<Penalty> defaultPenalties = penaltyRepository.findByUserUidIsNull();
        userPenalties.addAll(defaultPenalties);
        return userPenalties;
    }

    /**
     * 사용자가 새로운 벌칙을 생성합니다.
     */
    @Transactional
    public void createPenalty(String description, String userUid) {
        Penalty penalty = new Penalty(description, userUid);
        penaltyRepository.save(penalty);
    }

    /**
     * 사용자가 새로운 벌칙을 생성하고 ID를 반환합니다.
     */
    @Transactional
    public Long createPenalty(String description, String userUid, boolean returnId) {
        Penalty penalty = new Penalty(description, userUid);
        penalty = penaltyRepository.save(penalty);
        return penalty.getId();
    }

    /**
     * 사용자가 자신이 만든 벌칙을 수정합니다.
     */
    @Transactional
    public void updatePenalty(Long penaltyId, String newDescription, String userUid) {
        Penalty penalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new IllegalArgumentException("Penalty not found"));

        // 자신이 만든 벌칙만 수정 가능
        if (!userUid.equals(penalty.getUserUid())) {
            throw new IllegalStateException("You can only update your own penalties");
        }
        
        penalty.updateDescription(newDescription);
    }

    /**
     * 사용자가 자신이 만든 벌칙을 삭제합니다.
     */
    @Transactional
    public void deletePenalty(Long penaltyId, String userUid) {
        Penalty penalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new IllegalArgumentException("Penalty not found"));

        // 자신이 만든 벌칙이 아니면 삭제할 수 없습니다.
        if (!userUid.equals(penalty.getUserUid())) {
            throw new IllegalStateException("You can only delete your own penalties");
        }
        penaltyRepository.delete(penalty);
    }
}