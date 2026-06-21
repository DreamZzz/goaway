package com.goaway.contexts.activity.application;

import com.goaway.contexts.activity.api.dto.BadgeDTO;
import com.goaway.contexts.activity.domain.Badge;
import com.goaway.contexts.activity.domain.BadgeMetric;
import com.goaway.contexts.activity.domain.ActivityType;
import com.goaway.contexts.activity.domain.UserBadge;
import com.goaway.contexts.activity.infrastructure.persistence.ActivityEventRepository;
import com.goaway.contexts.activity.infrastructure.persistence.UserBadgeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class BadgeService {

    private final ActivityEventRepository activityRepo;
    private final UserBadgeRepository userBadgeRepo;

    public BadgeService(ActivityEventRepository activityRepo, UserBadgeRepository userBadgeRepo) {
        this.activityRepo = activityRepo;
        this.userBadgeRepo = userBadgeRepo;
    }

    /** 计算用户当前各指标值（一次性算齐，避免逐徽章重复查库）。 */
    private Map<BadgeMetric, Long> computeMetrics(Long userId) {
        Map<BadgeMetric, Long> m = new EnumMap<>(BadgeMetric.class);
        m.put(BadgeMetric.FISH_MAX_SECONDS, activityRepo.maxDurationByUserIdAndType(userId, ActivityType.FISH));
        m.put(BadgeMetric.POOP_MAX_SECONDS, activityRepo.maxDurationByUserIdAndType(userId, ActivityType.POOP));
        m.put(BadgeMetric.FISH_TOTAL_SECONDS, activityRepo.sumDurationByUserIdAndType(userId, ActivityType.FISH));
        m.put(BadgeMetric.WATER_TOTAL_COUNT, activityRepo.countByUserIdAndType(userId, ActivityType.WATER));
        m.put(BadgeMetric.SMOKE_TOTAL_COUNT, activityRepo.countByUserIdAndType(userId, ActivityType.SMOKE));
        m.put(BadgeMetric.POOP_TOTAL_COUNT, activityRepo.countByUserIdAndType(userId, ActivityType.POOP));
        return m;
    }

    /** 评估并发放新解锁的徽章（幂等：已得不重复发）。返回本次新解锁的 key。 */
    @Transactional
    public List<String> evaluateAndAward(Long userId) {
        Map<BadgeMetric, Long> metrics = computeMetrics(userId);
        List<String> newly = new ArrayList<>();
        for (Badge badge : Badge.values()) {
            long current = metrics.getOrDefault(badge.getMetric(), 0L);
            if (current >= badge.getThreshold() && !userBadgeRepo.existsByUserIdAndBadgeKey(userId, badge.getKey())) {
                userBadgeRepo.save(new UserBadge(userId, badge.getKey()));
                newly.add(badge.getKey());
            }
        }
        return newly;
    }

    /** 徽章墙：全部徽章（已得高亮 + 未得进度）。 */
    @Transactional
    public List<BadgeDTO> listBadges(Long userId) {
        evaluateAndAward(userId); // 懒补发，保证展示与数据一致
        Map<BadgeMetric, Long> metrics = computeMetrics(userId);
        Map<String, java.time.LocalDateTime> earned = new java.util.HashMap<>();
        for (UserBadge ub : userBadgeRepo.findByUserId(userId)) {
            earned.put(ub.getBadgeKey(), ub.getEarnedAt());
        }
        List<BadgeDTO> result = new ArrayList<>();
        for (Badge badge : Badge.values()) {
            long current = metrics.getOrDefault(badge.getMetric(), 0L);
            boolean isEarned = earned.containsKey(badge.getKey());
            double progress = badge.getThreshold() <= 0 ? 1.0
                    : Math.min(1.0, (double) current / badge.getThreshold());
            result.add(new BadgeDTO(badge, current, isEarned,
                    isEarned ? earned.get(badge.getKey()) : null, progress));
        }
        return result;
    }
}
