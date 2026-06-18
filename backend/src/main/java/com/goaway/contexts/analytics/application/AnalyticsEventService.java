package com.goaway.contexts.analytics.application;

import com.goaway.contexts.analytics.api.dto.AnalyticsEventRequest;
import com.goaway.platform.provider.analytics.AnalyticsEvent;
import com.goaway.platform.provider.analytics.AnalyticsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Forwards analytics events from the API layer to the configured {@link AnalyticsSender}.
 * Per-event failures are swallowed so a single bad event cannot break the batch.
 */
@Service
public class AnalyticsEventService {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsEventService.class);

    private final AnalyticsSender analyticsSender;

    public AnalyticsEventService(AnalyticsSender analyticsSender) {
        this.analyticsSender = analyticsSender;
    }

    public void recordEvent(AnalyticsEventRequest request, String userId) {
        if (request == null || request.getEventName() == null || request.getEventName().isBlank()) {
            return;
        }
        try {
            Map<String, Object> props = request.getProperties();
            analyticsSender.track(request.getEventName(), userId, props);
        } catch (Exception exception) {
            logger.warn("Failed to record analytics event {}: {}", request.getEventName(), exception.getMessage());
        }
    }

    public void recordBatch(List<AnalyticsEventRequest> events, String userId) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (AnalyticsEventRequest event : events) {
            recordEvent(event, userId);
        }
    }

    /**
     * Internal callers (e.g. AuthService) can report directly without constructing a DTO.
     */
    public void recordInternal(String eventName, String userId, Map<String, Object> properties) {
        try {
            analyticsSender.track(eventName, userId, properties);
        } catch (Exception exception) {
            logger.warn("Failed to record internal analytics event {}: {}", eventName, exception.getMessage());
        }
    }

    // kept for future use when we buffer and flush internally
    @SuppressWarnings("unused")
    private AnalyticsEvent toDomain(AnalyticsEventRequest request, String userId) {
        Instant occurredAt = request.getOccurredAt() != null ? request.getOccurredAt() : Instant.now();
        return new AnalyticsEvent(request.getEventName(), userId, request.getProperties(), occurredAt);
    }
}
