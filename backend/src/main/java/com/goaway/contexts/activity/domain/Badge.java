package com.goaway.contexts.activity.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * 荣誉徽章目录（静态定义，事实源）。基于单次动作（SINGLE）或累计动作（CUMULATIVE）解锁。
 * 新增徽章只需在此追加（枚举只增不改，符合向后兼容约定）。icon 对应前端 Icon 组件的 glyph 名。
 */
public enum Badge {

    // ── 单次 ──
    FISH_MARATHON("摸鱼马拉松", "单次摸鱼满 1 小时", BadgeKind.SINGLE, BadgeMetric.FISH_MAX_SECONDS, 3600, "fish"),
    FISH_ALLDAY("带薪一整天", "单次摸鱼满 4 小时", BadgeKind.SINGLE, BadgeMetric.FISH_MAX_SECONDS, 14400, "trophy"),
    POOP_THRONE("带薪王座", "单次带薪拉屎满 15 分钟", BadgeKind.SINGLE, BadgeMetric.POOP_MAX_SECONDS, 900, "toilet"),

    // ── 累计 ──
    FISH_STARTER("初级摸鱼", "累计摸鱼满 1 小时", BadgeKind.CUMULATIVE, BadgeMetric.FISH_TOTAL_SECONDS, 3600, "fish"),
    FISH_MASTER("摸鱼大师", "累计摸鱼满 100 小时", BadgeKind.CUMULATIVE, BadgeMetric.FISH_TOTAL_SECONDS, 360000, "trophy"),
    WATER_100("水逆体质", "累计喝水满 100 杯", BadgeKind.CUMULATIVE, BadgeMetric.WATER_TOTAL_COUNT, 100, "water"),
    SMOKE_100("老烟枪", "累计抽烟满 100 根", BadgeKind.CUMULATIVE, BadgeMetric.SMOKE_TOTAL_COUNT, 100, "smoke"),
    POOP_50("带薪如厕达人", "累计带薪拉屎满 50 次", BadgeKind.CUMULATIVE, BadgeMetric.POOP_TOTAL_COUNT, 50, "toilet");

    private final String title;
    private final String description;
    private final BadgeKind kind;
    private final BadgeMetric metric;
    private final long threshold;
    private final String icon;

    Badge(String title, String description, BadgeKind kind, BadgeMetric metric, long threshold, String icon) {
        this.title = title;
        this.description = description;
        this.kind = kind;
        this.metric = metric;
        this.threshold = threshold;
        this.icon = icon;
    }

    public String getKey() { return name(); }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BadgeKind getKind() { return kind; }
    public BadgeMetric getMetric() { return metric; }
    public long getThreshold() { return threshold; }
    public String getIcon() { return icon; }

    public static Optional<Badge> fromKey(String key) {
        return Arrays.stream(values()).filter(b -> b.name().equals(key)).findFirst();
    }
}
