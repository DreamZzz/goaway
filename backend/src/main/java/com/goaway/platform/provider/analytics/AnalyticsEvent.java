package com.goaway.platform.provider.analytics;

import java.time.Instant;
import java.util.Map;

/**
 * Internal carrier for a single analytics event passed from the API layer to the provider.
 */
public record AnalyticsEvent(
        String eventName,
        String userId,
        Map<String, Object> properties,
        Instant occurredAt
) {
}
