package com.goaway.contexts.analytics.api;

import com.goaway.contexts.analytics.api.dto.AnalyticsEventBatchRequest;
import com.goaway.contexts.analytics.api.dto.AnalyticsEventRequest;
import com.goaway.contexts.analytics.application.AnalyticsEventService;
import com.goaway.platform.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsEventController {

    private final AnalyticsEventService analyticsEventService;
    private final CurrentUserService currentUserService;

    public AnalyticsEventController(
            AnalyticsEventService analyticsEventService,
            CurrentUserService currentUserService) {
        this.analyticsEventService = analyticsEventService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/events")
    public ResponseEntity<Void> reportEvent(@Valid @RequestBody AnalyticsEventRequest request) {
        analyticsEventService.recordEvent(request, resolveUserId());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/events/batch")
    public ResponseEntity<Void> reportBatch(@Valid @RequestBody AnalyticsEventBatchRequest request) {
        analyticsEventService.recordBatch(request.getEvents(), resolveUserId());
        return ResponseEntity.accepted().build();
    }

    private String resolveUserId() {
        return currentUserService.getCurrentUser()
                .map(user -> user.getId() != null ? String.valueOf(user.getId()) : null)
                .orElse(null);
    }
}
