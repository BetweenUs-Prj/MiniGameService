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

    @Transactional(readOnly = true)
    public List<Penalty> getPenalties(String userUid) {
        return penaltyRepository.findByUserUid(userUid);
    }

    @Transactional
    public void createPenalty(String description, String userUid) {
        Penalty penalty = new Penalty(description, userUid);
        penaltyRepository.save(penalty);
    }

    @Transactional
    public void deletePenalty(Long penaltyId, String userUid) {
        Penalty penalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new IllegalArgumentException("Penalty not found"));

        if (!penalty.getUserUid().equals(userUid)) {
            throw new IllegalStateException("You can only delete your own penalties");
        }
        penaltyRepository.delete(penalty);
    }
}