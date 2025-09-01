package su.kdt.minigame.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import su.kdt.minigame.exception.InvalidOptionException;
import su.kdt.minigame.exception.RoundGoneException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiErrorAdvice {
    
    @ExceptionHandler(RoundGoneException.class)
    @ResponseStatus(HttpStatus.GONE)
    public Map<String, Object> handleRoundGone(RoundGoneException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", "ROUND_CLOSED");
        error.put("message", "Round is closed");
        error.put("phase", e.getPhase());
        error.put("submittedCount", e.getSubmittedCount());
        error.put("expectedParticipants", e.getExpectedParticipants());
        error.put("sessionId", e.getSessionId());
        error.put("roundId", e.getRoundId());
        return error;
    }
    
    @ExceptionHandler(InvalidOptionException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, Object> handleInvalidOption(InvalidOptionException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", "INVALID_OPTION");
        error.put("message", "Invalid option for this round");
        error.put("roundId", e.getRoundId());
        error.put("optionId", e.getOptionId());
        return error;
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", "CONFLICT");
        error.put("message", "Data conflict or duplicate entry");
        return error;
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleIllegalArgument(IllegalArgumentException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", "NOT_FOUND");
        error.put("message", e.getMessage());
        return error;
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGeneralException(Exception e) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", "INTERNAL_ERROR");
        error.put("message", "An unexpected error occurred");
        // Log the actual exception for debugging
        System.err.println("Unhandled exception: " + e.getMessage());
        e.printStackTrace();
        return error;
    }
}