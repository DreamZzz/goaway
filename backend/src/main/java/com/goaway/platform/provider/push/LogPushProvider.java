package com.goaway.platform.provider.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 本地默认推送实现：只把 payload 打到日志，不真正发 APNs。
 * 让定时生成 / 频控 / 深链 payload 在本地全链路可验证，无需真实证书。
 */
@Component
@ConditionalOnProperty(name = "app.push.provider", havingValue = "log", matchIfMissing = true)
public class LogPushProvider implements PushProvider {

    private static final Logger log = LoggerFactory.getLogger(LogPushProvider.class);

    @Override
    public ProviderMode providerMode() {
        return ProviderMode.LOG;
    }

    @Override
    public PushResult send(PushMessage message) {
        log.info("[push:log] token={} title={} body={} payload={}",
                maskToken(message.deviceToken()), message.title(), message.body(), message.payload());
        return PushResult.ok();
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "********";
        }
        return token.substring(0, 6) + "…" + token.substring(token.length() - 4);
    }
}
