package com.goaway.contexts.leaderboard.api;

import com.goaway.contexts.leaderboard.api.dto.BoardInfoDTO;
import com.goaway.contexts.leaderboard.api.dto.LeaderboardDTO;
import com.goaway.contexts.leaderboard.application.LeaderboardService;
import com.goaway.platform.security.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final CurrentUserService currentUserService;

    public LeaderboardController(LeaderboardService leaderboardService, CurrentUserService currentUserService) {
        this.leaderboardService = leaderboardService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/boards")
    public ResponseEntity<List<BoardInfoDTO>> boards() {
        return ResponseEntity.ok(leaderboardService.listBoards());
    }

    @GetMapping
    public ResponseEntity<LeaderboardDTO> get(
            @RequestParam(defaultValue = "fishing") String board,
            @RequestParam(defaultValue = "all") String dimension,
            @RequestParam(required = false) String slice,
            @RequestParam(defaultValue = "day") String period) {
        return ResponseEntity.ok(leaderboardService.load(board, dimension, slice, period, currentRealUserId()));
    }

    /** 登录用户返回真实 id 用于「我的排名」；游客 / 未登录返回 null。 */
    private Long currentRealUserId() {
        if (currentUserService.isGuest()) {
            return null;
        }
        return currentUserService.getCurrentUser()
                .map(user -> user.getId())
                .orElse(null);
    }
}
