package com.goaway.contexts.admin.api;

import com.goaway.contexts.activity.domain.BadgeDefinition;
import com.goaway.contexts.activity.domain.LeaderboardDefinition;
import com.goaway.contexts.admin.api.dto.AdminMetaDTO;
import com.goaway.contexts.admin.api.dto.BadgeDefInput;
import com.goaway.contexts.admin.api.dto.LeaderboardDefInput;
import com.goaway.contexts.admin.api.dto.RuleValidateRequest;
import com.goaway.contexts.admin.api.dto.RuleValidateResult;
import com.goaway.contexts.admin.application.AdminService;
import com.goaway.platform.security.AdminSecretGuard;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 运营后台 API。全部要求请求头 X-Admin-Secret == app.admin.secret（由 AdminSecretGuard 校验）。
 * 路径在 /admin/** 下：SecurityConfig 放行 Spring Security，由本守卫把关；AdminRateLimitFilter 限流。
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final AdminSecretGuard guard;

    public AdminController(AdminService adminService, AdminSecretGuard guard) {
        this.adminService = adminService;
        this.guard = guard;
    }

    @GetMapping("/meta")
    public AdminMetaDTO meta(@RequestHeader(value = "X-Admin-Secret", required = false) String secret) {
        guard.verify(secret);
        return adminService.meta();
    }

    @PostMapping("/rules/validate")
    public RuleValidateResult validate(@RequestHeader(value = "X-Admin-Secret", required = false) String secret,
                                       @RequestBody RuleValidateRequest req) {
        guard.verify(secret);
        return adminService.validate(req);
    }

    // ── 勋章 ──

    @GetMapping("/badges")
    public List<BadgeDefinition> listBadges(@RequestHeader(value = "X-Admin-Secret", required = false) String secret) {
        guard.verify(secret);
        return adminService.listBadges();
    }

    @PostMapping("/badges")
    public BadgeDefinition createBadge(@RequestHeader(value = "X-Admin-Secret", required = false) String secret,
                                       @RequestBody BadgeDefInput in) {
        guard.verify(secret);
        return adminService.saveBadge(null, in);
    }

    @PutMapping("/badges/{id}")
    public BadgeDefinition updateBadge(@RequestHeader(value = "X-Admin-Secret", required = false) String secret,
                                       @PathVariable Long id, @RequestBody BadgeDefInput in) {
        guard.verify(secret);
        return adminService.saveBadge(id, in);
    }

    @PostMapping("/badges/{id}/toggle")
    public BadgeDefinition toggleBadge(@RequestHeader(value = "X-Admin-Secret", required = false) String secret,
                                       @PathVariable Long id) {
        guard.verify(secret);
        return adminService.toggleBadge(id);
    }

    @DeleteMapping("/badges/{id}")
    public ResponseEntity<Void> deleteBadge(@RequestHeader(value = "X-Admin-Secret", required = false) String secret,
                                            @PathVariable Long id) {
        guard.verify(secret);
        adminService.deleteBadge(id);
        return ResponseEntity.noContent().build();
    }

    // ── 榜单 ──

    @GetMapping("/leaderboards")
    public List<LeaderboardDefinition> listBoards(@RequestHeader(value = "X-Admin-Secret", required = false) String secret) {
        guard.verify(secret);
        return adminService.listLeaderboards();
    }

    @PostMapping("/leaderboards")
    public LeaderboardDefinition createBoard(@RequestHeader(value = "X-Admin-Secret", required = false) String secret,
                                             @RequestBody LeaderboardDefInput in) {
        guard.verify(secret);
        return adminService.saveLeaderboard(null, in);
    }

    @PutMapping("/leaderboards/{id}")
    public LeaderboardDefinition updateBoard(@RequestHeader(value = "X-Admin-Secret", required = false) String secret,
                                             @PathVariable Long id, @RequestBody LeaderboardDefInput in) {
        guard.verify(secret);
        return adminService.saveLeaderboard(id, in);
    }

    @PostMapping("/leaderboards/{id}/toggle")
    public LeaderboardDefinition toggleBoard(@RequestHeader(value = "X-Admin-Secret", required = false) String secret,
                                             @PathVariable Long id) {
        guard.verify(secret);
        return adminService.toggleLeaderboard(id);
    }

    @DeleteMapping("/leaderboards/{id}")
    public ResponseEntity<Void> deleteBoard(@RequestHeader(value = "X-Admin-Secret", required = false) String secret,
                                            @PathVariable Long id) {
        guard.verify(secret);
        adminService.deleteLeaderboard(id);
        return ResponseEntity.noContent().build();
    }
}
