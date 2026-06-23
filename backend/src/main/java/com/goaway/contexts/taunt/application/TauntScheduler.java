package com.goaway.contexts.taunt.application;

import com.goaway.contexts.push.domain.PushPreference;
import com.goaway.contexts.push.infrastructure.persistence.PushPreferenceRepository;
import com.goaway.contexts.taunt.domain.TauntTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;

/**
 * 毒舌主动推送的调度入口（三类触发）：
 *  1) 定时巡检：按用户频率/间隔到期就发；
 *  2) 场景触发：周一早 / 周五傍晚等固定时刻；
 *  3) 不活跃召回：N 天没打开 App 的用户。
 * 千人千面的 LLM 生成放在有界线程池里跑，避免巡检时刻打满。
 */
@Component
public class TauntScheduler {

    private static final Logger log = LoggerFactory.getLogger(TauntScheduler.class);

    private final PushPreferenceRepository preferenceRepository;
    private final TauntService tauntService;
    private final boolean enabled;
    private final int recallInactiveDays;
    private final ExecutorService executor;

    public TauntScheduler(PushPreferenceRepository preferenceRepository,
                          TauntService tauntService,
                          @Value("${app.taunt.enabled:true}") boolean enabled,
                          @Value("${app.taunt.recall-inactive-days:3}") int recallInactiveDays,
                          @Value("${app.taunt.max-generate-concurrency:4}") int maxConcurrency) {
        this.preferenceRepository = preferenceRepository;
        this.tauntService = tauntService;
        this.enabled = enabled;
        this.recallInactiveDays = recallInactiveDays;
        this.executor = Executors.newFixedThreadPool(Math.max(1, maxConcurrency));
    }

    /** 定时巡检：默认每 10 分钟一轮。 */
    @Scheduled(fixedDelayString = "${app.taunt.sweep-interval-ms:600000}", initialDelay = 60_000)
    public void scheduledSweep() {
        if (!enabled) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        dispatch(preferenceRepository.findActivePushTargets(),
                pref -> tauntService.isDue(pref, now), TauntTrigger.SCHEDULED);
    }

    /** 周一 09:00 早高峰场景。 */
    @Scheduled(cron = "${app.taunt.cron-monday:0 0 9 * * MON}")
    public void mondayMorning() {
        sceneSweep(TauntTrigger.SCENE_MONDAY);
    }

    /** 周五 18:00 下班场景。 */
    @Scheduled(cron = "${app.taunt.cron-friday:0 0 18 * * FRI}")
    public void fridayEvening() {
        sceneSweep(TauntTrigger.SCENE_FRIDAY);
    }

    /** 每日 11:00 不活跃召回。 */
    @Scheduled(cron = "${app.taunt.cron-recall:0 0 11 * * *}")
    public void recallSweep() {
        if (!enabled) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusDays(recallInactiveDays);
        dispatch(preferenceRepository.findRecallTargets(threshold),
                pref -> tauntService.canSendNow(pref, now), TauntTrigger.RECALL);
    }

    private void sceneSweep(TauntTrigger trigger) {
        if (!enabled) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        dispatch(preferenceRepository.findActivePushTargets(),
                pref -> tauntService.canSendNow(pref, now), trigger);
    }

    private void dispatch(List<PushPreference> candidates, Predicate<PushPreference> gate, TauntTrigger trigger) {
        List<? extends Future<?>> futures = candidates.stream()
                .filter(gate)
                .map(pref -> executor.submit(() -> safeDeliver(pref.getUserId(), trigger)))
                .toList();
        if (!futures.isEmpty()) {
            log.info("Taunt dispatch trigger={} 命中 {} 个用户", trigger, futures.size());
        }
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                log.warn("Taunt dispatch 等待失败: {}", e.toString());
            }
        }
    }

    private void safeDeliver(Long userId, TauntTrigger trigger) {
        try {
            tauntService.deliver(userId, trigger);
        } catch (Exception e) {
            log.warn("Taunt deliver 异常 userId={} trigger={}: {}", userId, trigger, e.toString());
        }
    }
}
