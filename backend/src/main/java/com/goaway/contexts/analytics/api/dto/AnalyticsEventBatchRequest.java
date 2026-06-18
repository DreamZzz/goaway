package com.goaway.contexts.analytics.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Batch payload — the frontend fallback uploader buffers events and posts them together.
 */
public class AnalyticsEventBatchRequest {

    @NotEmpty
    @Size(max = 100)
    @Valid
    private List<AnalyticsEventRequest> events;

    public AnalyticsEventBatchRequest() {}

    public List<AnalyticsEventRequest> getEvents() {
        return events;
    }

    public void setEvents(List<AnalyticsEventRequest> events) {
        this.events = events;
    }
}
