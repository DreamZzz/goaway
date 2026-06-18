package com.goaway.contexts.analytics.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

/**
 * Payload for a single analytics event reported from the client.
 *
 * <p>Event names and property keys are owned by the frontend's
 * {@code src/shared/analytics/events.js} — keep them in sync manually.</p>
 */
public class AnalyticsEventRequest {

    @NotBlank
    @Size(max = 64)
    private String eventName;

    private Map<String, Object> properties;

    private Instant occurredAt;

    public AnalyticsEventRequest() {}

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
