package su.kdt.minigame.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class LeaderboardDto {
    
    private Long groupId;
    private List<LeaderboardEntry> entries;
    private int totalEntries;
    private int currentPage;
    private int totalPages;
    private long timestamp;
    
    @Getter
    @Builder
    public static class LeaderboardEntry {
        private String userUid;
        private String displayName;
        private Integer totalScore;
        private Integer correctCount;
        private Integer totalQuestions;
        private Long durationMs;
        private Integer rank;
        private LocalDateTime finishedAt;
        private boolean isCurrentUser;
    }
}