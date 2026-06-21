package com.goaway.contexts.activity.domain;

import com.goaway.contexts.activity.domain.rule.Metric;

/**
 * 内置勋章系列：一个系列对应一个指标，含 5 档递增阈值（与 {@link BadgeTier#ASCENDING} 一一对应）。
 * 达成更高档即「晋级」。每档的持有键为 "&lt;series&gt;.&lt;TIER&gt;"（如 water.NPC）。
 */
public enum BadgeSeries {

    FISH_TOTAL("fish_total", "摸鱼时长", "fish", Metric.FISH_TOTAL_SECONDS,
            new long[]{3600, 36000, 180000, 360000, 1800000}),        // 1h/10h/50h/100h/500h
    FISH_SINGLE("fish_single", "单次摸鱼", "fish", Metric.FISH_MAX_SECONDS,
            new long[]{1800, 3600, 7200, 14400, 28800}),              // 30m/1h/2h/4h/8h
    WATER("water", "喝水", "water", Metric.WATER_COUNT,
            new long[]{10, 50, 200, 500, 2000}),
    SMOKE("smoke", "抽烟", "smoke", Metric.SMOKE_COUNT,
            new long[]{10, 50, 200, 500, 2000}),
    POOP_COUNT("poop_count", "带薪如厕", "toilet", Metric.POOP_COUNT,
            new long[]{5, 20, 50, 150, 500}),
    POOP_SINGLE("poop_single", "单次带薪", "toilet", Metric.POOP_MAX_SECONDS,
            new long[]{300, 600, 900, 1800, 3600});                   // 5m/10m/15m/30m/60m

    private final String key;
    private final String title;
    private final String icon;
    private final Metric metric;
    private final long[] thresholds; // index 对应 BadgeTier.order

    BadgeSeries(String key, String title, String icon, Metric metric, long[] thresholds) {
        this.key = key;
        this.title = title;
        this.icon = icon;
        this.metric = metric;
        this.thresholds = thresholds;
    }

    public static java.util.Optional<BadgeSeries> byKey(String key) {
        for (BadgeSeries s : values()) if (s.key.equals(key)) return java.util.Optional.of(s);
        return java.util.Optional.empty();
    }

    public String getKey() { return key; }
    public String getTitle() { return title; }
    public String getIcon() { return icon; }
    public Metric getMetric() { return metric; }
    public long thresholdOf(BadgeTier tier) { return thresholds[tier.getOrder()]; }

    /** 每档的持有键。 */
    public String badgeKey(BadgeTier tier) { return key + "." + tier.name(); }
}
