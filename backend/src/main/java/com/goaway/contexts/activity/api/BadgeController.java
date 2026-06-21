package com.goaway.contexts.activity.api;

import com.goaway.contexts.activity.api.dto.BadgeDTO;
import com.goaway.contexts.activity.application.BadgeService;
import com.goaway.platform.security.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/badges")
public class BadgeController {

    private final BadgeService badgeService;
    private final CurrentUserService currentUserService;

    public BadgeController(BadgeService badgeService, CurrentUserService currentUserService) {
        this.badgeService = badgeService;
        this.currentUserService = currentUserService;
    }

    /** 荣誉墙：全部徽章（已解锁 + 未解锁带进度）。 */
    @GetMapping
    public ResponseEntity<List<BadgeDTO>> list() {
        Long userId = currentUserService.requireRealUserId();
        return ResponseEntity.ok(badgeService.listBadges(userId));
    }
}
