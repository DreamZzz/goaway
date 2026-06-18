package com.goaway.platform.provider.analytics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * No-op analytics sender — used as emergency kill-switch via
 * {@code app.analytics.provider=noop}.
 */
@Component
@ConditionalOnProperty(name = "app.analytics.provider", havingValue = "noop")
public class NoopAnalyticsSender implements AnalyticsSender {
    @Override
    public ProviderMode providerMode() {
        return ProviderMode.NOOP;
    }

    @Override
    public void track(String eventName, String userId, Map<String, Object> properties) {
        // intentionally empty
    }
}
