package com.goaway.contexts.fishing.api;

import com.goaway.contexts.fishing.api.dto.FishingReportRequest;
import com.goaway.contexts.fishing.api.dto.FishingSummaryDTO;
import com.goaway.contexts.fishing.application.FishingService;
import com.goaway.platform.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fishing")
public class FishingController {

    private final FishingService fishingService;
    private final CurrentUserService currentUserService;

    public FishingController(FishingService fishingService, CurrentUserService currentUserService) {
        this.fishingService = fishingService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/report")
    public ResponseEntity<FishingSummaryDTO> report(@Valid @RequestBody FishingReportRequest request) {
        Long userId = currentUserService.requireRealUserId();
        return ResponseEntity.ok(fishingService.report(userId, request.getSeconds()));
    }

    @GetMapping("/summary")
    public ResponseEntity<FishingSummaryDTO> summary() {
        Long userId = currentUserService.requireRealUserId();
        return ResponseEntity.ok(fishingService.summary(userId));
    }
}
