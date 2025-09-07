package su.kdt.minigame.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 플레이어 점수 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerScore {
    private String userUid;
    private int totalScore;
    private int correctCount;
    private int totalAnswers;
    private int rank;
}