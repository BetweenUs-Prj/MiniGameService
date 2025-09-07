package su.kdt.minigame.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 게임 결과 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameResultDto {
    
    private Long sessionId;
    private List<PlayerScore> players;
    private String winnerUid;
    private Long penaltyId;
    private String penaltyText;
    private String status;
    private String message;
    private Long timestamp;
    
    /**
     * 정상 결과 생성
     */
    public static GameResultDto of(Long sessionId, List<PlayerScore> players, String winnerUid, Long penaltyId) {
        return GameResultDto.builder()
            .sessionId(sessionId)
            .players(players)
            .winnerUid(winnerUid)
            .penaltyId(penaltyId)
            .status("FINISHED")
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 대기 상태 응답 (410 GONE)
     */
    public static GameResultDto waiting(String currentStatus) {
        return GameResultDto.builder()
            .status(currentStatus)
            .message("Game not finished yet")
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 에러 응답 (422)
     */
    public static GameResultDto error(String message) {
        return GameResultDto.builder()
            .status("ERROR")
            .message(message)
            .timestamp(System.currentTimeMillis())
            .build();
    }
}