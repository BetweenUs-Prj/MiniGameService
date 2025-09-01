package su.kdt.minigame.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 점수판 DTO (폴링용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreboardDto {
    
    private Long sessionId;
    private Long timestamp;
    private List<ScoreEntry> scores;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreEntry {
        private int rank;
        private String userUid;
        private int score;
        private int correctCount;
    }
}