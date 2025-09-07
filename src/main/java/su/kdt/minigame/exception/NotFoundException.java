package su.kdt.minigame.exception;

/**
 * 404 Not Found 예외
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}