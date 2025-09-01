package su.kdt.minigame.dto.request;

import jakarta.validation.constraints.NotNull;

public record SubmitAnswerReq(
    String userUid, // Set by server from header, optional in frontend request
    @NotNull Long sessionId,
    @NotNull Long optionId, // Frontend sends optionId, not choiceIndex
    Long responseTimeMs // Client-provided response time in milliseconds
) {
    // Constructor for server-side creation with userUid from header
    public SubmitAnswerReq(String userUid, Long optionId) {
        this(userUid, null, optionId, null); // sessionId, responseTimeMs not needed for processing
    }
    
    // Legacy choiceIndex for service compatibility
    public Integer choiceIndex() {
        return optionId != null ? optionId.intValue() : null;
    }
    
    // Legacy field for backward compatibility
    public String answerText() {
        return optionId != null ? String.valueOf(optionId) : null;
    }
}