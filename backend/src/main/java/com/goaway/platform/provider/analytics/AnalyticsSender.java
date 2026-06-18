package com.goaway.platform.provider.analytics;

import java.util.List;
import java.util.Map;

/**
 * Swappable analytics provider. Business code depends only on this interface;
 * implementations are selected via {@code app.analytics.provider}.
 */
public interface AnalyticsSender {
    enum ProviderMode {
        LOG,
        UMENG,
        NOOP
    }

    ProviderMode providerMode();

    /**
     * Report a single event. Implementations MUST swallow all exceptions — a failed
     * analytics report must never break the business flow.
     */
    void track(String eventName, String userId, Map<String, Object> properties);

    /**
     * Report a batch of events. Default implementation loops over {@link #track}.
     */
    default void trackBatch(List<AnalyticsEvent> events) {
        if (events == null) return;
        for (AnalyticsEvent event : events) {
            if (event == null || event.eventName() == null) continue;
            track(event.eventName(), event.userId(), event.properties());
        }
    }
}
