package com.goaway.contexts.workprofile.api;

import com.goaway.contexts.workprofile.api.dto.UpsertWorkProfileRequest;
import com.goaway.contexts.workprofile.api.dto.WorkProfileDTO;
import com.goaway.contexts.workprofile.application.WorkProfileService;
import com.goaway.platform.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile/work")
public class WorkProfileController {

    private final WorkProfileService workProfileService;
    private final CurrentUserService currentUserService;

    public WorkProfileController(WorkProfileService workProfileService, CurrentUserService currentUserService) {
        this.workProfileService = workProfileService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<WorkProfileDTO> getMyProfile() {
        Long userId = currentUserService.requireRealUserId();
        return ResponseEntity.ok(WorkProfileDTO.from(workProfileService.getOrEmpty(userId)));
    }

    @PutMapping
    public ResponseEntity<WorkProfileDTO> upsert(@Valid @RequestBody UpsertWorkProfileRequest request) {
        Long userId = currentUserService.requireRealUserId();
        return ResponseEntity.ok(WorkProfileDTO.from(workProfileService.upsert(userId, request)));
    }
}
