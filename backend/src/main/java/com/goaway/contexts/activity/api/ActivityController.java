package com.goaway.contexts.activity.api;

import com.goaway.contexts.activity.api.dto.ActivitySummaryDTO;
import com.goaway.contexts.activity.api.dto.RecordActivityRequest;
import com.goaway.contexts.activity.application.ActivityService;
import com.goaway.platform.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityService activityService;
    private final CurrentUserService currentUserService;

    public ActivityController(ActivityService activityService, CurrentUserService currentUserService) {
        this.activityService = activityService;
        this.currentUserService = currentUserService;
    }

    /** 记录一次离散动作（喝水/抽烟/拉屎），异步基础数据。 */
    @PostMapping("/events")
    public ResponseEntity<Void> record(@Valid @RequestBody RecordActivityRequest request) {
        Long userId = currentUserService.requireRealUserId();
        activityService.record(userId, request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<ActivitySummaryDTO> summary() {
        Long userId = currentUserService.requireRealUserId();
        return ResponseEntity.ok(activityService.todaySummary(userId));
    }
}
