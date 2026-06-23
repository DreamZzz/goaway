package com.goaway.contexts.taunt.application;

import com.goaway.contexts.push.application.PushService;
import com.goaway.contexts.push.domain.PushFrequency;
import com.goaway.contexts.push.domain.PushPreference;
import com.goaway.contexts.taunt.domain.TauntLog;
import com.goaway.contexts.taunt.domain.TauntTrigger;
import com.goaway.contexts.taunt.infrastructure.persistence.TauntLogRepository;
import com.goaway.platform.provider.push.PushMessage;
import com.goaway.platform.provider.push.PushProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 单个用户的毒舌推送投递：频控判定 → 生成内容 → 批量推送到其设备 → 记录日志/水位。
 * 不持有跨 APNs HTTP 的事务，DB 写各自小事务提交。
 */
@Service
public class TauntService {

    private static final Logger log = LoggerFactory.getLogger(TauntService.class);
    private static final String TITLE = "💢 有人 cue 你";

    private final TauntContentService contentService;
    private final PushService pushService;
    private final PushProvider pushProvider;
    private final TauntLogRepository tauntLogRepository;

    public TauntService(TauntContentService contentService, PushService pushService,
                        PushProvider pushProvider, TauntLogRepository tauntLogRepository) {
        this.contentService = contentService;
        this.pushService = pushService;
        this.pushProvider = pushProvider;
        this.tauntLogRepository = tauntLogRepository;
    }

    /**
     * 基础可发判定（开启 + 非 OFF + 非免打扰 + 未超每日上限），不含间隔。
     * 场景触发 / 不活跃召回用它（这些是特殊时机，可越过间隔，但仍守上限与免打扰）。
     */
    public boolean canSendNow(PushPreference pref, LocalDateTime now) {
        PushFrequency freq = pref.getFrequency();
        if (!pref.isEnabled() || freq == null || freq.isOff()) {
            return false;
        }
        if (pref.isQuietHour(now.getHour())) {
            return false;
        }
        long todayCount = tauntLogRepository.countByUserIdAndSuccessTrueAndSentAtAfter(
                pref.getUserId(), now.toLocalDate().atStartOfDay());
        return todayCount < freq.dailyCap();
    }

    /** 定时巡检用：在 canSendNow 基础上再加「距上次满足频率间隔」。 */
    public boolean isDue(PushPreference pref, LocalDateTime now) {
        if (!canSendNow(pref, now)) {
            return false;
        }
        PushFrequency freq = pref.getFrequency();
        return pref.getLastTauntAt() == null
                || Duration.between(pref.getLastTauntAt(), now).toMinutes() >= freq.intervalMinutes();
    }

    /** 投递结果：实际生成的文案 + 是否至少一台设备成功。 */
    public record DeliverResult(String content, boolean success) {}

    /**
     * 生成并投递一条毒舌推送。返回生成的文案与是否至少一台设备成功。
     */
    public DeliverResult deliver(Long userId, TauntTrigger trigger) {
        List<String> tokens = pushService.deviceTokensForUser(userId);
        String content = contentService.generate(userId, trigger);
        if (tokens.isEmpty()) {
            return new DeliverResult(content, false);
        }
        Map<String, String> payload = Map.of("type", "taunt", "trigger", trigger.name());

        List<PushMessage> messages = tokens.stream()
                .map(t -> PushMessage.of(t, TITLE, content, payload))
                .toList();

        List<PushProvider.PushResult> results = pushProvider.sendBatch(messages);

        boolean anySuccess = false;
        StringBuilder detail = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            PushProvider.PushResult r = results.get(i);
            anySuccess |= r.success();
            if (r.invalidToken()) {
                pushService.removeDevice(tokens.get(i));
            }
            if (!r.success() && detail.length() < 150) {
                detail.append(r.detail()).append(';');
            }
        }

        tauntLogRepository.save(new TauntLog(userId, trigger, content, anySuccess,
                anySuccess ? "ok" : detail.toString()));
        if (anySuccess) {
            pushService.markTaunted(userId);
        }
        log.debug("Taunt 投递 userId={} trigger={} tokens={} success={}", userId, trigger, tokens.size(), anySuccess);
        return new DeliverResult(content, anySuccess);
    }
}
