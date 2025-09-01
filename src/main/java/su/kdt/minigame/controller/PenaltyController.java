package su.kdt.minigame.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.domain.Penalty;
import su.kdt.minigame.service.PenaltyService;
import su.kdt.minigame.support.UidResolverFilter;


import java.util.List;
import su.kdt.minigame.dto.PenaltyDto;
import su.kdt.minigame.repository.PenaltyRepository;

// DTO(Data Transfer Object) 정의
record UpdatePenaltyReq(@NotBlank String description) {} // 수정용 DTO
record PenaltyCreateDto(@NotBlank String description) {} // 생성용 DTO

@RestController
@RequestMapping("/api/penalties")
@RequiredArgsConstructor
public class PenaltyController {

    private final PenaltyService penaltyService;
    private final PenaltyRepository penaltyRepository;

    /**
     * 벌칙 생성
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PenaltyDto> create(@Valid @RequestBody PenaltyCreateDto dto,
                                             HttpServletRequest request) {
        String uid = (String) request.getAttribute(UidResolverFilter.ATTR_UID);
        // 유저가 null일 경우 system 벌칙으로 저장
        if (uid == null) {
            uid = "system";
        }
        
        try {
            Penalty p = penaltyService.create(uid, dto.description());
            return ResponseEntity.status(HttpStatus.CREATED).body(PenaltyDto.from(p));
        } catch (Exception e) {
            throw new IllegalArgumentException("벌칙 생성에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 벌칙 목록 조회 (스코프별)
     */
    @GetMapping
    public List<PenaltyDto> list(
            @RequestParam(defaultValue = "all") String scope,
            HttpServletRequest req
    ) {
        String uid = (String) req.getAttribute(UidResolverFilter.ATTR_UID);
        List<Penalty> items = switch(scope) {
            case "mine" -> penaltyRepository.findByUserUid(uid);
            case "system" -> penaltyRepository.findByUserUid("system");
            default -> penaltyRepository.findByUserUidOrUserUid(uid, "system");
        };
        return items.stream().map(PenaltyDto::from).toList();
    }

    /**
     * 내 벌칙 수정
     */
    @PutMapping("/{penaltyId}")
    public ResponseEntity<Void> updatePenalty(
            HttpServletRequest httpRequest,
            @PathVariable Long penaltyId,
            @RequestBody UpdatePenaltyReq req
    ) {
        String userUid = (String) httpRequest.getAttribute(UidResolverFilter.ATTR_UID);
        penaltyService.updatePenalty(penaltyId, req.description(), userUid);
        return ResponseEntity.ok().build();
    }

    /**
     * 내 벌칙 삭제
     */
    @DeleteMapping("/{penaltyId}")
    public ResponseEntity<Void> deletePenalty(
            HttpServletRequest httpRequest,
            @PathVariable Long penaltyId
    ) {
        String userUid = (String) httpRequest.getAttribute(UidResolverFilter.ATTR_UID);
        penaltyService.deletePenalty(penaltyId, userUid);
        return ResponseEntity.noContent().build();
    }
}