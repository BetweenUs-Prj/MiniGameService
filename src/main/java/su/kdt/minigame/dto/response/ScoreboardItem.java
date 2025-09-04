package su.kdt.minigame.dto.response;

/**
 * 스코어보드 항목 DTO - 퀴즈 게임 점수 현황 표시용
 */
public record ScoreboardItem(
    String userUid,    // 사용자 UID
    int score,         // 현재 점수
    int correctCount,  // 정답 개수
    int totalAnswered, // 총 답변 개수
    int rank           // 현재 순위 (1등부터)
) {
    // Backward compatibility method
    public String userId() { return userUid; }
    /**
     * 기본 생성자 - 빈 점수로 초기화
     */
    public static ScoreboardItem empty(String userUid) {
        return new ScoreboardItem(userUid, 0, 0, 0, 999);
    }
    
    /**
     * 점수만으로 생성 (간단한 스코어보드용)
     */
    public static ScoreboardItem withScore(String userUid, int score, int rank) {
        return new ScoreboardItem(userUid, score, 0, 0, rank);
    }
}