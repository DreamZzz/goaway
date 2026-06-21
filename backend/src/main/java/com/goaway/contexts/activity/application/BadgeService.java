package com.goaway.contexts.activity.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goaway.contexts.activity.api.dto.BadgeAwardDTO;
import com.goaway.contexts.activity.api.dto.BadgeDTO;
import com.goaway.contexts.activity.api.dto.BadgeSeriesDTO;
import com.goaway.contexts.activity.api.dto.BadgeTierItemDTO;
import com.goaway.contexts.activity.api.dto.BadgeWallDTO;
import com.goaway.contexts.activity.application.rule.RuleEvaluator;
import com.goaway.contexts.activity.domain.BadgeDefinition;
import com.goaway.contexts.activity.domain.BadgeSeries;
import com.goaway.contexts.activity.domain.BadgeTier;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /** 规则引擎指标快照（按 Metric 目录 key 聚合 activity_events）。 */
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

    private Set<String> earnedKeys(Long userId) {
        Set<String> s = new HashSet<>();
        for (UserBadge ub : userBadgeRepo.findByUserId(userId)) s.add(ub.getBadgeKey());
        return s;
    }

    /**
     * 评估并发放新解锁档位（内置系列 + 配置勋章；幂等）。
     * 系列：跨过的每档都落库（保留各档获得日期），但每系列只返回「最高新档」用于弹窗，避免连弹。
     */
    @Transactional
    public List<BadgeAwardDTO> evaluateAndAward(Long userId) {
        Map<String, Long> snap = metricSnapshot(userId);
        Set<String> owned = earnedKeys(userId);
        List<BadgeAwardDTO> awards = new ArrayList<>();

        for (BadgeSeries series : BadgeSeries.values()) {
            long value = snap.getOrDefault(series.getMetric().getKey(), 0L);
            boolean hadBefore = ownedAnyTier(owned, series);
            BadgeAwardDTO best = null;
            for (BadgeTier tier : BadgeTier.ASCENDING) {
                if (value < series.thresholdOf(tier)) break;        // 阈值递增，后面更高也不满足
                String key = series.badgeKey(tier);
                if (owned.contains(key)) continue;
                UserBadge saved = userBadgeRepo.save(new UserBadge(userId, key));
                owned.add(key);
                best = new BadgeAwardDTO(series.getKey(), series.getTitle(), series.getIcon(),
                        tier.name(), tier.getLabel(), tier.getOrder(), tier.getColorKey(),
                        series.thresholdOf(tier), series.getMetric().getUnit(),
                        saved.getEarnedAt(), hadBefore); // 之前有过该系列 → 晋级
            }
            if (best != null) awards.add(best);
        }

        // 配置勋章（单枚，无档位）
        for (BadgeDefinition def : badgeDefRepo.findByEnabledTrueOrderBySortOrderAscIdAsc()) {
            if (owned.contains(def.getKey())) continue;
            try {
                Rule rule = objectMapper.readValue(def.getRuleJson(), Rule.class);
                if (ruleEvaluator.evalRule(rule, snap)) {
                    UserBadge saved = userBadgeRepo.save(new UserBadge(userId, def.getKey()));
                    owned.add(def.getKey());
                    awards.add(new BadgeAwardDTO(def.getKey(), def.getTitle(),
                            def.getIcon() == null ? "trophy" : def.getIcon(),
                            "", "", 0, "lav", 0, "count", saved.getEarnedAt(), false));
                }
            } catch (Exception e) {
                log.warn("跳过配置勋章 {}（解析/求值失败）: {}", def.getKey(), e.getMessage());
            }
        }
        return awards;
    }

    private boolean ownedAnyTier(Set<String> owned, BadgeSeries series) {
        for (BadgeTier tier : BadgeTier.ASCENDING) {
            if (owned.contains(series.badgeKey(tier))) return true;
        }
        return false;
    }

    /** 勋章墙：系列（档位/晋级进度）+ 配置勋章 extras。 */
    @Transactional
    public BadgeWallDTO listWall(Long userId) {
        evaluateAndAward(userId); // 懒补发，保证展示一致
        Map<String, Long> snap = metricSnapshot(userId);
        Map<String, LocalDateTime> earned = new HashMap<>();
        for (UserBadge ub : userBadgeRepo.findByUserId(userId)) earned.put(ub.getBadgeKey(), ub.getEarnedAt());

        List<BadgeSeriesDTO> seriesList = new ArrayList<>();
        for (BadgeSeries series : BadgeSeries.values()) {
            long value = snap.getOrDefault(series.getMetric().getKey(), 0L);
            BadgeSeriesDTO dto = new BadgeSeriesDTO();
            dto.setSeriesKey(series.getKey());
            dto.setTitle(series.getTitle());
            dto.setIcon(series.getIcon());
            dto.setUnit(series.getMetric().getUnit());
            dto.setCurrent(value);

            List<BadgeTierItemDTO> tiers = new ArrayList<>();
            BadgeTier currentTier = null;
            LocalDateTime currentEarnedAt = null;
            BadgeTier nextTier = null;
            for (BadgeTier tier : BadgeTier.ASCENDING) {
                String key = series.badgeKey(tier);
                boolean isEarned = earned.containsKey(key);
                LocalDateTime at = earned.get(key);
                tiers.add(new BadgeTierItemDTO(tier.name(), tier.getLabel(), tier.getColorKey(),
                        series.thresholdOf(tier), isEarned, at));
                if (isEarned) { currentTier = tier; currentEarnedAt = at; }
                else if (nextTier == null) { nextTier = tier; }
            }
            dto.setTiers(tiers);
            dto.setCurrentTier(currentTier == null ? null : currentTier.name());
            dto.setCurrentTierLabel(currentTier == null ? null : currentTier.getLabel());
            dto.setCurrentTierOrder(currentTier == null ? -1 : currentTier.getOrder());
            dto.setCurrentColorKey(currentTier == null ? null : currentTier.getColorKey());
            dto.setCurrentEarnedAt(currentEarnedAt);
            if (nextTier == null) {
                dto.setNextTierLabel(null);
                dto.setNextThreshold(0);
                dto.setProgressToNext(1.0);
            } else {
                long nt = series.thresholdOf(nextTier);
                dto.setNextTierLabel(nextTier.getLabel());
                dto.setNextThreshold(nt);
                dto.setProgressToNext(nt <= 0 ? 1.0 : Math.min(1.0, (double) value / nt));
            }
            seriesList.add(dto);
        }

        List<BadgeDTO> extras = new ArrayList<>();
        for (BadgeDefinition def : badgeDefRepo.findByEnabledTrueOrderBySortOrderAscIdAsc()) {
            extras.add(buildConfigBadgeDTO(def, snap, earned.containsKey(def.getKey()), earned.get(def.getKey())));
        }
        return new BadgeWallDTO(seriesList, extras);
    }

    /** 配置勋章 DTO：尽量从首个「指标 ≥/> 常量」条件推断进度，否则按是否解锁给 0/1。 */
    private BadgeDTO buildConfigBadgeDTO(BadgeDefinition def, Map<String, Long> snap,
                                         boolean earned, LocalDateTime earnedAt) {
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
            // 复杂规则不推断进度
        }
        return new BadgeDTO(def.getKey(), def.getTitle(), def.getDescription(),
                def.getKind() == null ? "CUMULATIVE" : def.getKind(),
                def.getIcon() == null ? "trophy" : def.getIcon(),
                unit, threshold, current, earned, earnedAt, progress);
    }
}
