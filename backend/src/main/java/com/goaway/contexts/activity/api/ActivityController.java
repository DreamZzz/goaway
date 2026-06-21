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

    /** 记录一次离散动作（喝水/抽烟/拉屎/摸鱼），返回本次新解锁的勋章档位（中奖弹窗用）。 */
    @PostMapping("/events")
    public ResponseEntity<java.util.List<com.goaway.contexts.activity.api.dto.BadgeAwardDTO>> record(
            @Valid @RequestBody RecordActivityRequest request) {
        Long userId = currentUserService.requireRealUserId();
        return ResponseEntity.ok(activityService.record(userId, request));
    }

    @GetMapping("/summary")
    public ResponseEntity<ActivitySummaryDTO> summary() {
        Long userId = currentUserService.requireRealUserId();
        return ResponseEntity.ok(activityService.todaySummary(userId));
    }
}
