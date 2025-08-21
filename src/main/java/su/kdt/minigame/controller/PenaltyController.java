package su.kdt.minigame.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.domain.Penalty;
import su.kdt.minigame.service.PenaltyService;

import java.util.List;

// DTO(Data Transfer Object) 정의
record CreatePenaltyReq(String description) {}
record UpdatePenaltyReq(String description) {} // 수정용 DTO 추가

@RestController
@RequestMapping("/api/penalties")
@RequiredArgsConstructor
public class PenaltyController {

    private final PenaltyService penaltyService;

    /**
     * 내 벌칙 목록 조회 (기본 벌칙 포함)
     */
    @GetMapping
    public ResponseEntity<List<Penalty>> getMyPenalties(@RequestHeader("X-USER-UID") String userUid) {
        List<Penalty> penalties = penaltyService.getPenalties(userUid);
        return ResponseEntity.ok(penalties);
    }

    /**
     * 내 벌칙 목록에 새 벌칙 추가
     */
    @PostMapping
    public ResponseEntity<Void> createPenalty(
            @RequestHeader("X-USER-UID") String userUid, 
            @RequestBody CreatePenaltyReq req
    ) {
        penaltyService.createPenalty(req.description(), userUid);
        return ResponseEntity.ok().build();
    }

    /**
     * 내 벌칙 수정
     */
    @PutMapping("/{penaltyId}") // ◀◀◀ 이 메소드가 추가되었습니다.
    public ResponseEntity<Void> updatePenalty(
            @RequestHeader("X-USER-UID") String userUid,
            @PathVariable Long penaltyId,
            @RequestBody UpdatePenaltyReq req
    ) {
        penaltyService.updatePenalty(penaltyId, req.description(), userUid);
        return ResponseEntity.ok().build();
    }

    /**
     * 내 벌칙 삭제
     */
    @DeleteMapping("/{penaltyId}")
    public ResponseEntity<Void> deletePenalty(
            @RequestHeader("X-USER-UID") String userUid, 
            @PathVariable Long penaltyId
    ) {
        penaltyService.deletePenalty(penaltyId, userUid);
        return ResponseEntity.noContent().build();
    }
}