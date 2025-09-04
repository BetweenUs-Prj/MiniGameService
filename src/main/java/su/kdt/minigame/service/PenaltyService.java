package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import su.kdt.minigame.domain.Penalty;
import su.kdt.minigame.repository.PenaltyRepository;


import java.util.List;

@Service
@RequiredArgsConstructor
public class PenaltyService {

    private final PenaltyRepository penaltyRepository;

    /**
     * 요구사항에 맞는 벌칙 생성 메소드
     */
    @Transactional
    public Penalty create(Long uid, String text) {
        if (text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "벌칙 내용을 입력해주세요.");
        }
        // 선택사항: 중복 방지
        // if (penaltyRepository.existsByUserUidAndText(uid, text)) {
        //     throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 같은 벌칙이 있어요.");
        // }
        var p = new Penalty(text, uid);
        return penaltyRepository.save(p);
    }

    /**
     * 특정 사용자가 볼 수 있는 모든 벌칙 목록을 조회합니다.
     * (사용자 정의 벌칙 + 기본 벌칙)
     */
    @Transactional(readOnly = true)
    public List<Penalty> getPenalties(Long userId) {
        List<Penalty> userPenalties = penaltyRepository.findByUserId(userId);
        List<Penalty> defaultPenalties = penaltyRepository.findByUserIdIsNull();
        userPenalties.addAll(defaultPenalties);
        return userPenalties;
    }

    /**
     * 사용자가 새로운 벌칙을 생성합니다.
     */
    @Transactional
    public void createPenalty(String description, Long userId) {
        Penalty penalty = new Penalty(description, userId);
        penaltyRepository.save(penalty);
    }

    /**
     * 사용자가 새로운 벌칙을 생성하고 ID를 반환합니다.
     */
    @Transactional
    public Long createPenalty(String description, Long userId, boolean returnId) {
        Penalty penalty = new Penalty(description, userId);
        penalty = penaltyRepository.save(penalty);
        return penalty.getPenaltyId();
    }

    /**
     * 사용자가 새로운 벌칙을 생성하고 생성된 엔티티를 반환합니다.
     */
    @Transactional
    public Penalty createPenaltyAndReturn(String description, Long userId) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("벌칙 내용을 입력해주세요.");
        }
        Penalty penalty = new Penalty(description, userId);
        return penaltyRepository.save(penalty);
    }

    /**
     * 사용자가 자신이 만든 벌칙을 수정합니다.
     */
    @Transactional
    public void updatePenalty(Long penaltyId, String newDescription, Long userId) {
        Penalty penalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new IllegalArgumentException("Penalty not found"));

        // 자신이 만든 벌칙만 수정 가능
        if (!userId.equals(penalty.getUserUid())) {
            throw new IllegalStateException("You can only update your own penalties");
        }
        
        penalty.setText(newDescription);
    }

    /**
     * 사용자가 자신이 만든 벌칙을 삭제합니다.
     */
    @Transactional
    public void deletePenalty(Long penaltyId, Long userId) {
        Penalty penalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new IllegalArgumentException("Penalty not found"));

        // 자신이 만든 벌칙이 아니면 삭제할 수 없습니다.
        if (!userId.equals(penalty.getUserUid())) {
            throw new IllegalStateException("You can only delete your own penalties");
        }
        penaltyRepository.delete(penalty);
    }

    /**
     * ID로 벌칙을 조회합니다.
     */
    @Transactional(readOnly = true)
    public Penalty getPenaltyById(Long penaltyId) {
        return penaltyRepository.findById(penaltyId).orElse(null);
    }
}