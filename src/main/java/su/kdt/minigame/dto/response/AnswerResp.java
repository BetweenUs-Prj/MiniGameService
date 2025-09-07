package su.kdt.minigame.dto.response;

public record AnswerResp(
    boolean correct,
    int score,
    int totalScore,
    boolean allSubmitted,
    int submittedCount,
    int expectedParticipants,
    boolean alreadySubmitted
) {
    
    public static AnswerResp createAlreadySubmitted() {
        return new AnswerResp(false, 0, 0, false, 0, 0, true);
    }
    
    public static AnswerResp ok(boolean correct, int score, int totalScore, boolean allSubmitted, 
                               int submittedCount, int expectedParticipants) {
        return new AnswerResp(correct, score, totalScore, allSubmitted, submittedCount, expectedParticipants, false);
    }
    
    // Builder-style methods for updating specific fields
    public AnswerResp withSubmittedCount(int submittedCount) {
        return new AnswerResp(this.correct, this.score, this.totalScore, this.allSubmitted, 
                              submittedCount, this.expectedParticipants, this.alreadySubmitted);
    }
    
    public AnswerResp withExpectedParticipants(int expectedParticipants) {
        return new AnswerResp(this.correct, this.score, this.totalScore, this.allSubmitted, 
                              this.submittedCount, expectedParticipants, this.alreadySubmitted);
    }
    
    public AnswerResp withAllSubmitted(boolean allSubmitted) {
        return new AnswerResp(this.correct, this.score, this.totalScore, allSubmitted, 
                              this.submittedCount, this.expectedParticipants, this.alreadySubmitted);
    }
}