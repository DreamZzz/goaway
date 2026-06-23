package com.goaway.platform.provider.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * APNs token 鉴权（.p8）+ JDK HttpClient HTTP/2 直连，避免引入重依赖。
 * topic = 应用 bundle id（com.868299.goaway）。生产 production=true 走 api.push.apple.com。
 */
@Component
@ConditionalOnProperty(name = "app.push.provider", havingValue = "apns")
public class ApnsPushProvider implements PushProvider {

    private static final Logger log = LoggerFactory.getLogger(ApnsPushProvider.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ApnsJwt apnsJwt;
    private final String topic;
    private final String host;

    public ApnsPushProvider(
            @Value("${app.push.apns.key-p8:}") String keyP8,
            @Value("${app.push.apns.key-id:}") String keyId,
            @Value("${app.push.apns.team-id:}") String teamId,
            @Value("${app.push.apns.topic:com.868299.goaway}") String topic,
            @Value("${app.push.apns.production:true}") boolean production) {
        if (keyP8 == null || keyP8.isBlank()) {
            throw new IllegalStateException("app.push.provider=apns 但 app.push.apns.key-p8 为空");
        }
        this.apnsJwt = new ApnsJwt(keyP8, keyId, teamId);
        this.topic = topic;
        this.host = production ? "https://api.push.apple.com" : "https://api.sandbox.push.apple.com";
        log.info("ApnsPushProvider 就绪 host={} topic={}", host, topic);
    }

    @Override
    public ProviderMode providerMode() {
        return ProviderMode.APNS;
    }

    @Override
    public PushResult send(PushMessage message) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(host + "/3/device/" + message.deviceToken()))
                    .timeout(Duration.ofSeconds(15))
                    .header("authorization", "bearer " + apnsJwt.getToken())
                    .header("apns-topic", topic)
                    .header("apns-push-type", "alert")
                    .header("apns-priority", "10")
                    .POST(HttpRequest.BodyPublishers.ofString(buildPayload(message)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code == 200) {
                return PushResult.ok();
            }
            // 410 Unregistered / 400 BadDeviceToken → token 失效，调用方删设备
            if (code == 410 || (code == 400 && response.body() != null && response.body().contains("BadDeviceToken"))) {
                return PushResult.invalid("apns " + code + " " + response.body());
            }
            log.warn("APNs 推送失败 code={} body={}", code, response.body());
            return PushResult.fail("apns " + code);
        } catch (Exception e) {
            log.warn("APNs 推送异常 token=…{}: {}", tail(message.deviceToken()), e.toString());
            return PushResult.fail(e.getMessage());
        }
    }

    private String buildPayload(PushMessage message) {
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode aps = root.putObject("aps");
        ObjectNode alert = aps.putObject("alert");
        alert.put("title", message.title() == null ? "" : message.title());
        alert.put("body", message.body() == null ? "" : message.body());
        aps.put("sound", "default");
        for (Map.Entry<String, String> e : message.payload().entrySet()) {
            root.put(e.getKey(), e.getValue());
        }
        return root.toString();
    }

    private String tail(String token) {
        return token == null || token.length() < 4 ? "????" : token.substring(token.length() - 4);
    }
}
