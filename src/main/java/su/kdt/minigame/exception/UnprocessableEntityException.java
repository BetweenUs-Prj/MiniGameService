package su.kdt.minigame.exception;

/**
 * 422 Unprocessable Entity 예외
 */
public class UnprocessableEntityException extends RuntimeException {
    public UnprocessableEntityException(String message) {
        super(message);
    }
}