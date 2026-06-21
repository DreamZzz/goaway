package com.goaway.contexts.activity.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goaway.contexts.activity.api.dto.BadgeDTO;
import com.goaway.contexts.activity.application.rule.RuleEvaluator;
import com.goaway.contexts.activity.domain.Badge;
import com.goaway.contexts.activity.domain.BadgeDefinition;
import com.goaway.contexts.activity.domain.BadgeMetric;
import com.goaway.contexts.activity.domain.ActivityType;
import com.goaway.contexts.activity.domain.UserBadge;
import com.goaway.contexts.activity.domain.rule.Condition;
import com.goaway.contexts.activity.domain.rule.Expr;
import com.goaway.contexts.activity.domain.rule.Metric;
import com.goaway.contexts.activity.domain.rule.Rule;
import com.goaway.contexts.activity.infrastructure.persistence.ActivityEventRepository;
import com.goaway.contexts.activity.infrastructure.persistence.BadgeDefinitionRepository;
import com.goaway.contexts.activity.infrastructure.persistence.UserBadgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BadgeService {

    private static final Logger log = LoggerFactory.getLogger(BadgeService.class);

    private final ActivityEventRepository activityRepo;
    private final UserBadgeRepository userBadgeRepo;
    private final BadgeDefinitionRepository badgeDefRepo;
    private final RuleEvaluator ruleEvaluator;
    private final ObjectMapper objectMapper;

    public BadgeService(ActivityEventRepository activityRepo, UserBadgeRepository userBadgeRepo,
                        BadgeDefinitionRepository badgeDefRepo, RuleEvaluator ruleEvaluator,
                        ObjectMapper objectMapper) {
        this.activityRepo = activityRepo;
        this.userBadgeRepo = userBadgeRepo;
        this.badgeDefRepo = badgeDefRepo;
        this.ruleEvaluator = ruleEvaluator;
        this.objectMapper = objectMapper;
    }

    /** 内置勋章指标（旧 BadgeMetric 枚举口径）。 */
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

    /** 规则引擎指标快照（按 Metric 目录 key），供配置勋章/校验用。 */
    private Map<String, Long> metricSnapshot(Long userId) {
        Map<String, Long> snap = new HashMap<>();
        for (Metric m : Metric.values()) {
            long v = switch (m.getAgg()) {
                case COUNT -> activityRepo.countByUserIdAndType(userId, m.getType());
                case SUM -> activityRepo.sumDurationByUserIdAndType(userId, m.getType());
                case MAX -> activityRepo.maxDurationByUserIdAndType(userId, m.getType());
            };
            snap.put(m.getKey(), v);
        }
        return snap;
    }

    /** 评估并发放新解锁的徽章（内置 + 配置；幂等）。返回本次新解锁的 key。 */
    @Transactional
    public List<String> evaluateAndAward(Long userId) {
        List<String> newly = new ArrayList<>();
        // 内置
        Map<BadgeMetric, Long> metrics = computeMetrics(userId);
        for (Badge badge : Badge.values()) {
            long current = metrics.getOrDefault(badge.getMetric(), 0L);
            if (current >= badge.getThreshold() && !userBadgeRepo.existsByUserIdAndBadgeKey(userId, badge.getKey())) {
                userBadgeRepo.save(new UserBadge(userId, badge.getKey()));
                newly.add(badge.getKey());
            }
        }
        // 配置
        Map<String, Long> snap = metricSnapshot(userId);
        for (BadgeDefinition def : badgeDefRepo.findByEnabledTrueOrderBySortOrderAscIdAsc()) {
            try {
                Rule rule = objectMapper.readValue(def.getRuleJson(), Rule.class);
                if (ruleEvaluator.evalRule(rule, snap)
                        && !userBadgeRepo.existsByUserIdAndBadgeKey(userId, def.getKey())) {
                    userBadgeRepo.save(new UserBadge(userId, def.getKey()));
                    newly.add(def.getKey());
                }
            } catch (Exception e) {
                log.warn("跳过配置勋章 {}（规则解析/求值失败）: {}", def.getKey(), e.getMessage());
            }
        }
        return newly;
    }

    /** 徽章墙：内置 + 配置勋章（已得高亮 + 未得进度）。 */
    @Transactional
    public List<BadgeDTO> listBadges(Long userId) {
        evaluateAndAward(userId);
        Map<BadgeMetric, Long> metrics = computeMetrics(userId);
        Map<String, java.time.LocalDateTime> earned = new HashMap<>();
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
        // 配置勋章
        Map<String, Long> snap = metricSnapshot(userId);
        for (BadgeDefinition def : badgeDefRepo.findByEnabledTrueOrderBySortOrderAscIdAsc()) {
            boolean isEarned = earned.containsKey(def.getKey());
            result.add(buildConfigBadgeDTO(def, snap, isEarned,
                    isEarned ? earned.get(def.getKey()) : null));
        }
        return result;
    }

    /** 配置勋章 DTO：尽量从首个「指标 ≥/> 常量」条件推断进度，否则按是否解锁给 0/1。 */
    private BadgeDTO buildConfigBadgeDTO(BadgeDefinition def, Map<String, Long> snap,
                                         boolean earned, java.time.LocalDateTime earnedAt) {
        long current = 0;
        long threshold = 0;
        String unit = "count";
        double progress = earned ? 1.0 : 0.0;
        try {
            Rule rule = objectMapper.readValue(def.getRuleJson(), Rule.class);
            Condition c0 = rule.getConditions().get(0);
            Expr left = c0.getLeft();
            Expr right = c0.getRight();
            if (left != null && "metric".equals(left.getType())
                    && right != null && "const".equals(right.getType())) {
                Metric m = Metric.byKey(left.getMetric()).orElse(null);
                if (m != null) {
                    current = snap.getOrDefault(m.getKey(), 0L);
                    threshold = Math.round(right.getValue());
                    unit = m.getUnit();
                    if (threshold > 0) progress = Math.min(1.0, (double) current / threshold);
                }
            }
        } catch (Exception ignored) {
            // 复杂规则无法简单推断进度，保留 earned ? 1 : 0
        }
        return new BadgeDTO(def.getKey(), def.getTitle(), def.getDescription(),
                def.getKind() == null ? "CUMULATIVE" : def.getKind(),
                def.getIcon() == null ? "trophy" : def.getIcon(),
                unit, threshold, current, earned, earnedAt, progress);
    }
}
