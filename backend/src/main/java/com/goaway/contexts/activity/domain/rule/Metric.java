package com.goaway.contexts.activity.domain.rule;

import com.goaway.contexts.activity.domain.ActivityType;

import java.util.Arrays;
import java.util.Optional;

/**
 * 指标白名单目录（规则引擎的唯一合法叶子之一）。
 * 每个指标 = 对 activity_events 按用户聚合的一个口径，既给出 SQL 片段（榜单编译用），
 * 也给出 Agg+ActivityType（Java 快照求值用）。规则里出现目录外的 metric 直接报错，杜绝注入。
 */
public enum Metric {
    WATER_COUNT("water_count", "喝水次数", "count", ActivityType.WATER, Agg.COUNT),
    SMOKE_COUNT("smoke_count", "抽烟次数", "count", ActivityType.SMOKE, Agg.COUNT),
    POOP_COUNT("poop_count", "带薪拉屎次数", "count", ActivityType.POOP, Agg.COUNT),
    FISH_COUNT("fish_count", "摸鱼次数", "count", ActivityType.FISH, Agg.COUNT),
    FISH_TOTAL_SECONDS("fish_total_seconds", "累计摸鱼时长", "seconds", ActivityType.FISH, Agg.SUM),
    POOP_TOTAL_SECONDS("poop_total_seconds", "累计带薪时长", "seconds", ActivityType.POOP, Agg.SUM),
    FISH_MAX_SECONDS("fish_max_seconds", "单次最长摸鱼", "seconds", ActivityType.FISH, Agg.MAX),
    POOP_MAX_SECONDS("poop_max_seconds", "单次最长带薪", "seconds", ActivityType.POOP, Agg.MAX);

    private final String key;
    private final String label;
    private final String unit;
    private final ActivityType type;
    private final Agg agg;

    Metric(String key, String label, String unit, ActivityType type, Agg agg) {
        this.key = key;
        this.label = label;
        this.unit = unit;
        this.type = type;
        this.agg = agg;
    }

    public String getKey() { return key; }
    public String getLabel() { return label; }
    public String getUnit() { return unit; }
    public ActivityType getType() { return type; }
    public Agg getAgg() { return agg; }

    /** 该指标对应的 SQL 聚合片段（别名 t = activity_events）。仅由枚举常量拼成，无外部输入。 */
    public String sqlFragment() {
        String typeFilter = " FILTER (WHERE t.type = '" + type.name() + "')";
        return switch (agg) {
            case COUNT -> "COUNT(*)" + typeFilter;
            case SUM -> "COALESCE(SUM(t.duration_seconds)" + typeFilter + ", 0)";
            case MAX -> "COALESCE(MAX(t.duration_seconds)" + typeFilter + ", 0)";
        };
    }

    public static Optional<Metric> byKey(String key) {
        if (key == null) return Optional.empty();
        return Arrays.stream(values()).filter(m -> m.key.equals(key)).findFirst();
    }
}
