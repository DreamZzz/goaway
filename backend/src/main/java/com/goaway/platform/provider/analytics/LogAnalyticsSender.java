package com.goaway.platform.provider.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Default analytics sender — prints events to logs. Active when
 * {@code app.analytics.provider=log} or the property is missing.
 */
@Component
@ConditionalOnProperty(name = "app.analytics.provider", havingValue = "log", matchIfMissing = true)
public class LogAnalyticsSender implements AnalyticsSender {
    private static final Logger logger = LoggerFactory.getLogger(LogAnalyticsSender.class);

    @Override
    public ProviderMode providerMode() {
        return ProviderMode.LOG;
    }

    @Override
    public void track(String eventName, String userId, Map<String, Object> properties) {
        try {
            logger.info("[analytics] event={} userId={} props={}", eventName, userId, properties);
        } catch (Exception ignored) {
            // analytics must never break business flow
        }
    }
}
