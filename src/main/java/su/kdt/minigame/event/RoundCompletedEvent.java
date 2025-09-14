package su.kdt.minigame.event;

/**
 * 라운드 완료 이벤트 - 트랜잭션 커밋 후 다음 라운드 생성을 위해 발행
 */
public record RoundCompletedEvent(
    Long sessionId,
    Long roundId, 
    int roundNo,
    int totalPlayers
) {}