package com.goaway.contexts.weekly.api;

import com.goaway.contexts.weekly.api.dto.GenerateWeeklyRequest;
import com.goaway.contexts.weekly.api.dto.WeeklyReportDTO;
import com.goaway.contexts.weekly.application.WeeklyService;
import com.goaway.platform.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/weekly/reports")
public class WeeklyController {

    private final WeeklyService weeklyService;
    private final CurrentUserService currentUserService;

    public WeeklyController(WeeklyService weeklyService, CurrentUserService currentUserService) {
        this.weeklyService = weeklyService;
        this.currentUserService = currentUserService;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateStream(@Valid @RequestBody GenerateWeeklyRequest request) {
        Long userId = currentUserService.requireRealUserId();
        return weeklyService.streamGenerate(userId, request.getFragments());
    }

    @GetMapping
    public ResponseEntity<List<WeeklyReportDTO>> list() {
        Long userId = currentUserService.requireRealUserId();
        List<WeeklyReportDTO> reports = weeklyService.listRecent(userId).stream()
                .map(WeeklyReportDTO::from)
                .toList();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeeklyReportDTO> get(@PathVariable Long id) {
        Long userId = currentUserService.requireRealUserId();
        return weeklyService.get(userId, id)
                .map(WeeklyReportDTO::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "周报不存在"));
    }
}
