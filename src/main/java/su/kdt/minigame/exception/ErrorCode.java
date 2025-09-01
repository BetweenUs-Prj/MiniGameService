package su.kdt.minigame.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // Session related errors (4xx)
    SESSION_NOT_FOUND("SESSION_NOT_FOUND", "세션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SESSION_FULL("SESSION_FULL", "정원이 가득 찼습니다.", HttpStatus.CONFLICT),
    SESSION_CLOSED("SESSION_CLOSED", "세션이 종료되었습니다.", HttpStatus.GONE),
    INVALID_SESSION_STATUS("INVALID_SESSION_STATUS", "참가할 수 없는 상태의 세션입니다.", HttpStatus.CONFLICT),
    ALREADY_JOINED("ALREADY_JOINED", "이미 세션에 참여중입니다.", HttpStatus.CONFLICT),
    
    // Authorization errors (403)
    NOT_HOST("NOT_HOST", "호스트만 가능합니다.", HttpStatus.FORBIDDEN),
    INVITE_ONLY("INVITE_ONLY", "초대 링크로만 입장할 수 있어요.", HttpStatus.FORBIDDEN),
    PRIVATE_ROOM_PIN_REQUIRED("PRIVATE_ROOM_PIN_REQUIRED", "비공개방 PIN이 필요합니다.", HttpStatus.FORBIDDEN),
    INVALID_PIN("INVALID_PIN", "PIN이 올바르지 않습니다.", HttpStatus.FORBIDDEN),
    
    // Member related errors (4xx)  
    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", "대상이 없습니다.", HttpStatus.NOT_FOUND),
    CANNOT_KICK_HOST("CANNOT_KICK_HOST", "호스트는 강퇴할 수 없습니다.", HttpStatus.CONFLICT),
    
    // Game logic errors (422)
    NOT_ENOUGH_PLAYERS("NOT_ENOUGH_PLAYERS", "2명 이상 필요합니다.", HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_GAME_STATE("INVALID_GAME_STATE", "게임 상태가 올바르지 않습니다.", HttpStatus.UNPROCESSABLE_ENTITY),
    
    // Validation errors (400)
    INVALID_CODE_FORMAT("INVALID_CODE_FORMAT", "코드 형식이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_PIN_FORMAT("INVALID_PIN_FORMAT", "PIN은 4자리 숫자여야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST("INVALID_REQUEST", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    
    // Penalty related errors
    PENALTY_NOT_FOUND("PENALTY_NOT_FOUND", "벌칙을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    UNAUTHORIZED_PENALTY_ACCESS("UNAUTHORIZED_PENALTY_ACCESS", "벌칙에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    
    // Generic errors
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getStatus() {
        return status;
    }
}