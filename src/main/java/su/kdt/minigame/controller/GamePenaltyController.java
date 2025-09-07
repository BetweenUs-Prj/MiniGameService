package su.kdt.minigame.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.dto.CreatePenaltyReq;
import su.kdt.minigame.dto.CreatePenaltyRes;
import su.kdt.minigame.service.PenaltyService;
import su.kdt.minigame.support.UidResolverFilter;

@RestController
@RequestMapping("/api/mini-games/penalties")
@RequiredArgsConstructor
public class GamePenaltyController {
    
    private final PenaltyService penaltyService;

    @PostMapping
    public ResponseEntity<CreatePenaltyRes> create(
            HttpServletRequest httpRequest,
            @RequestBody @Valid CreatePenaltyReq req
    ) {
        String uidStr = (String) httpRequest.getAttribute(UidResolverFilter.ATTR_UID);
        Long userId = Long.valueOf(uidStr);
        // 요구사항에 맞는 create 메소드 사용
        var saved = penaltyService.create(userId, req.getText().trim());
        CreatePenaltyRes response = new CreatePenaltyRes(
            saved.getPenaltyId(), 
            saved.getText(), 
            String.valueOf(saved.getUserId()), 
            saved.getCreatedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}