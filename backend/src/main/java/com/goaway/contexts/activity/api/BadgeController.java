package com.goaway.contexts.activity.api;

import com.goaway.contexts.activity.api.dto.BadgeWallDTO;
import com.goaway.contexts.activity.application.BadgeService;
import com.goaway.platform.security.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/badges")
public class BadgeController {

    private final BadgeService badgeService;
    private final CurrentUserService currentUserService;

    public BadgeController(BadgeService badgeService, CurrentUserService currentUserService) {
        this.badgeService = badgeService;
        this.currentUserService = currentUserService;
    }

    /** 荣誉墙：内置系列（档位/晋级）+ 配置勋章 extras。 */
    @GetMapping
    public ResponseEntity<BadgeWallDTO> wall() {
        Long userId = currentUserService.requireRealUserId();
        return ResponseEntity.ok(badgeService.listWall(userId));
    }
}
