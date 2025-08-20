package su.kdt.minigame.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.domain.Penalty;
import su.kdt.minigame.service.PenaltyService;

import java.util.List;

record CreatePenaltyReq(String description) {}

@RestController
@RequestMapping("/api/penalties")
@RequiredArgsConstructor
public class PenaltyController {

    private final PenaltyService penaltyService;

    // 내 벌칙 목록 조회
    @GetMapping
    public ResponseEntity<List<Penalty>> getMyPenalties(@RequestHeader("X-USER-UID") String userUid) {
        List<Penalty> penalties = penaltyService.getPenalties(userUid);
        return ResponseEntity.ok(penalties);
    }

    // 내 벌칙 목록에 추가
    @PostMapping
    public ResponseEntity<Void> createPenalty(@RequestHeader("X-USER-UID") String userUid, @RequestBody CreatePenaltyReq req) {
        penaltyService.createPenalty(req.description(), userUid);
        return ResponseEntity.ok().build();
    }

    // 내 벌칙 삭제
    @DeleteMapping("/{penaltyId}")
    public ResponseEntity<Void> deletePenalty(@RequestHeader("X-USER-UID") String userUid, @PathVariable Long penaltyId) {
        penaltyService.deletePenalty(penaltyId, userUid);
        return ResponseEntity.noContent().build();
    }
}