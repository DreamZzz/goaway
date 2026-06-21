package com.goaway.contexts.activity.domain;

/** 徽章类别：基于单次动作 / 基于累计动作。 */
public enum BadgeKind {
    SINGLE,      // 单次：某一次动作达到阈值（如单次摸鱼满 1 小时）
    CUMULATIVE   // 累计：历史累计达到阈值（如累计喝水 100 杯）
}
