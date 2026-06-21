package com.goaway.contexts.activity.api.dto;

import java.time.LocalDateTime;

/** 一次新解锁的档位（用于中奖弹窗）。isPromotion=true 表示是更高档晋级。 */
public class BadgeAwardDTO {
    private String seriesKey;
    private String seriesTitle;
    private String icon;
    private String tier;        // 档位枚举名 (LA/NPC/...)
    private String tierLabel;   // 拉/NPC/人上人/顶级/夯
    private int tierOrder;
    private String colorKey;
    private long threshold;
    private String unit;        // seconds | count
    private LocalDateTime earnedAt;
    private boolean promotion;

    public BadgeAwardDTO() {}

    public BadgeAwardDTO(String seriesKey, String seriesTitle, String icon, String tier, String tierLabel,
                         int tierOrder, String colorKey, long threshold, String unit,
                         LocalDateTime earnedAt, boolean promotion) {
        this.seriesKey = seriesKey;
        this.seriesTitle = seriesTitle;
        this.icon = icon;
        this.tier = tier;
        this.tierLabel = tierLabel;
        this.tierOrder = tierOrder;
        this.colorKey = colorKey;
        this.threshold = threshold;
        this.unit = unit;
        this.earnedAt = earnedAt;
        this.promotion = promotion;
    }

    public String getSeriesKey() { return seriesKey; }
    public String getSeriesTitle() { return seriesTitle; }
    public String getIcon() { return icon; }
    public String getTier() { return tier; }
    public String getTierLabel() { return tierLabel; }
    public int getTierOrder() { return tierOrder; }
    public String getColorKey() { return colorKey; }
    public long getThreshold() { return threshold; }
    public String getUnit() { return unit; }
    public LocalDateTime getEarnedAt() { return earnedAt; }
    public boolean isPromotion() { return promotion; }
}
