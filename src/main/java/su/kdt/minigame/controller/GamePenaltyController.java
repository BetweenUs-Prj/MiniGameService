package su.kdt.minigame.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.dto.CreatePenaltyReq;
import su.kdt.minigame.dto.CreatePenaltyRes;
import su.kdt.minigame.service.PenaltyService;

@RestController
@RequestMapping("/api/mini-games/penalties")
@RequiredArgsConstructor
public class GamePenaltyController {
    
    private final PenaltyService penaltyService;

    @PostMapping
    public ResponseEntity<CreatePenaltyRes> create(
            @RequestBody @Valid CreatePenaltyReq req,
            @RequestHeader(value = "X-USER-UID", required = false) String userUid
    ) {
        // Use text field instead of description for new API
        Long id = penaltyService.createPenalty(req.getText().trim(), userUid, true);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatePenaltyRes(id));
    }
}