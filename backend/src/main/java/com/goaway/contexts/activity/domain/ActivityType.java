package com.goaway.contexts.activity.domain;

/**
 * 离散打工人动作类型。摸鱼（连续计时）走 fishing_sessions，这里记录离散事件。
 */
public enum ActivityType {
    WATER,   // 喝水 +1 杯
    SMOKE,   // 抽烟 +1 根
    POOP,    // 带薪拉屎（带 durationSeconds）
    FISH     // 摸鱼单次会话（带 durationSeconds，离开摸鱼页记录，用于「单次最长摸鱼」）
}
