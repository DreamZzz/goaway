package com.goaway.platform.provider.push;

import java.util.List;

/**
 * 远程推送通道抽象。本地默认 {@code log}（仅打印），生产切 {@code apns}。
 * 业务代码只依赖本接口，由 {@code app.push.provider} 选择实现。
 */
public interface PushProvider {

    enum ProviderMode {
        LOG, APNS
    }

    ProviderMode providerMode();

    /**
     * 发送单条推送。返回是否投递成功（失败不抛异常，便于批量场景统计）。
     */
    PushResult send(PushMessage message);

    /**
     * 批量发送。默认逐条调用，APNs 实现可复用同一连接/JWT。
     */
    default List<PushResult> sendBatch(List<PushMessage> messages) {
        return messages.stream().map(this::send).toList();
    }

    /**
     * 单条投递结果。
     * @param success 是否成功
     * @param detail  失败原因 / 状态码，成功时为 "ok"
     * @param invalidToken token 已失效（如 410 Unregistered），调用方应删除该设备
     */
    record PushResult(boolean success, String detail, boolean invalidToken) {
        public static PushResult ok() {
            return new PushResult(true, "ok", false);
        }

        public static PushResult fail(String detail) {
            return new PushResult(false, detail, false);
        }

        public static PushResult invalid(String detail) {
            return new PushResult(false, detail, true);
        }
    }
}
