package su.kdt.minigame.exception;

/**
 * 라운드에 속하지 않는 잘못된 옵션 ID를 제출할 때 발생하는 예외
 */
public class InvalidOptionException extends RuntimeException {
    private final long roundId;
    private final long optionId;
    
    public InvalidOptionException(long roundId, long optionId) {
        super(String.format("Invalid option %d for round %d", optionId, roundId));
        this.roundId = roundId;
        this.optionId = optionId;
    }
    
    public long getRoundId() { return roundId; }
    public long getOptionId() { return optionId; }
}