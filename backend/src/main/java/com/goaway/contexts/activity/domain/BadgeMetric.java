package com.goaway.contexts.activity.domain;

/**
 * 徽章衡量的指标维度。unit 决定前端如何格式化阈值/进度（时长 or 次数）。
 */
public enum BadgeMetric {
    FISH_MAX_SECONDS("seconds"),     // 单次最长摸鱼
    POOP_MAX_SECONDS("seconds"),     // 单次最长带薪拉屎
    FISH_TOTAL_SECONDS("seconds"),   // 累计摸鱼时长
    WATER_TOTAL_COUNT("count"),      // 累计喝水次数
    SMOKE_TOTAL_COUNT("count"),      // 累计抽烟次数
    POOP_TOTAL_COUNT("count");       // 累计带薪拉屎次数

    private final String unit;

    BadgeMetric(String unit) { this.unit = unit; }

    public String getUnit() { return unit; }
}
