package com.goaway.contexts.activity.domain.rule;

/** 指标聚合方式（对 activity_events 按用户聚合）。 */
public enum Agg {
    COUNT,  // 次数
    SUM,    // 累计时长
    MAX     // 单次最长
}
