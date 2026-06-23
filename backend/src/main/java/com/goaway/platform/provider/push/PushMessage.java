package com.goaway.platform.provider.push;

import java.util.Map;

/**
 * 一条待发送的推送：目标设备 token + 标题/正文 + 业务 payload（深链用，如 type=taunt）。
 */
public record PushMessage(String deviceToken, String title, String body, Map<String, String> payload) {

    public PushMessage {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public static PushMessage of(String deviceToken, String title, String body, Map<String, String> payload) {
        return new PushMessage(deviceToken, title, body, payload);
    }
}
