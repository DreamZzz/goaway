package com.goaway.platform.provider.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Umeng (友盟 U-App) server-side sender. Active when
 * {@code app.analytics.provider=umeng}.
 *
 * <p>Umeng's primary data ingestion path is the on-device RN SDK; this server-side
 * sender exists so backend-triggered events (server-signed login, cache hits) can be
 * reported out-of-band. If the app-key is blank we silently degrade to a log entry
 * rather than throwing — analytics must never break the business flow.</p>
 *
 * <p>Implementation note: a full Umeng server-push signing client is intentionally
 * out of scope for this first iteration. The skeleton here makes the wiring explicit
 * and leaves a clear extension point.</p>
 */
@Component
@ConditionalOnProperty(name = "app.analytics.provider", havingValue = "umeng")
public class UmengAnalyticsSender implements AnalyticsSender {
    private static final Logger logger = LoggerFactory.getLogger(UmengAnalyticsSender.class);

    private final String appKey;
    private final String appMasterSecret;

    public UmengAnalyticsSender(
            @Value("${app.analytics.umeng.app-key:}") String appKey,
            @Value("${app.analytics.umeng.app-master-secret:}") String appMasterSecret) {
        this.appKey = appKey;
        this.appMasterSecret = appMasterSecret;
    }

    @Override
    public ProviderMode providerMode() {
        return ProviderMode.UMENG;
    }

    @Override
    public void track(String eventName, String userId, Map<String, Object> properties) {
        try {
            if (appKey == null || appKey.isBlank()) {
                logger.info("[analytics:umeng-degraded] event={} userId={} props={}", eventName, userId, properties);
                return;
            }
            // TODO: implement signed POST to https://msg.umeng.com/api/send when
            //       the backend-side event spec is finalized. Intentionally skipped
            //       in the first iteration — the RN SDK is the primary ingestion path.
            logger.info("[analytics:umeng] event={} userId={} props={}", eventName, userId, properties);
        } catch (Exception exception) {
            logger.warn("Umeng analytics track failed for event {}: {}", eventName, exception.getMessage());
        }
    }
}
