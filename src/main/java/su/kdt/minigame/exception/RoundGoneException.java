package su.kdt.minigame.exception;

/**
 * 라운드가 이미 종료되었거나 전환 중인 상태에서 답변 제출을 시도할 때 발생하는 예외
 */
public class RoundGoneException extends RuntimeException {
    private final long sessionId;
    private final long roundId;
    private final String phase;
    private final int submittedCount;
    private final int expectedParticipants;
    
    public RoundGoneException(long sessionId, long roundId, String phase, int submittedCount, int expectedParticipants) {
        super(String.format("Round %d in session %d is closed (phase: %s)", roundId, sessionId, phase));
        this.sessionId = sessionId;
        this.roundId = roundId;
        this.phase = phase;
        this.submittedCount = submittedCount;
        this.expectedParticipants = expectedParticipants;
    }
    
    public long getSessionId() { return sessionId; }
    public long getRoundId() { return roundId; }
    public String getPhase() { return phase; }
    public int getSubmittedCount() { return submittedCount; }
    public int getExpectedParticipants() { return expectedParticipants; }
}