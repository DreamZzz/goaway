package com.goaway.contexts.checkin.api;

import com.goaway.contexts.checkin.api.dto.CheckinSummaryDTO;
import com.goaway.contexts.checkin.application.CheckinService;
import com.goaway.platform.security.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkin")
public class CheckinController {

    private final CheckinService checkinService;
    private final CurrentUserService currentUserService;

    public CheckinController(CheckinService checkinService, CurrentUserService currentUserService) {
        this.checkinService = checkinService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<CheckinSummaryDTO> checkin() {
        Long userId = currentUserService.requireRealUserId();
        return ResponseEntity.ok(checkinService.checkin(userId));
    }

    @GetMapping("/summary")
    public ResponseEntity<CheckinSummaryDTO> summary() {
        Long userId = currentUserService.requireRealUserId();
        return ResponseEntity.ok(checkinService.summary(userId));
    }
}
