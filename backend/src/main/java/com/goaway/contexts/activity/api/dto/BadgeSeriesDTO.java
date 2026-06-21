package com.goaway.contexts.activity.api.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 勋章墙的一个系列：当前最高档 + 下一档进度 + 5 档轨。 */
public class BadgeSeriesDTO {

    private String seriesKey;
    private String title;
    private String icon;
    private String unit;            // seconds | count
    private long current;           // 当前指标值
    private String currentTier;     // 当前最高档枚举名，未达任何档为 null
    private String currentTierLabel;
    private int currentTierOrder;   // -1 表示未达任何档
    private String currentColorKey;
    private LocalDateTime currentEarnedAt;
    private String nextTierLabel;   // 下一档名，已满级为 null
    private long nextThreshold;     // 下一档阈值，已满级为 0
    private double progressToNext;  // 0..1，已满级为 1
    private List<BadgeTierItemDTO> tiers;

    // getters
    public String getSeriesKey() { return seriesKey; }
    public void setSeriesKey(String v) { this.seriesKey = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getIcon() { return icon; }
    public void setIcon(String v) { this.icon = v; }
    public String getUnit() { return unit; }
    public void setUnit(String v) { this.unit = v; }
    public long getCurrent() { return current; }
    public void setCurrent(long v) { this.current = v; }
    public String getCurrentTier() { return currentTier; }
    public void setCurrentTier(String v) { this.currentTier = v; }
    public String getCurrentTierLabel() { return currentTierLabel; }
    public void setCurrentTierLabel(String v) { this.currentTierLabel = v; }
    public int getCurrentTierOrder() { return currentTierOrder; }
    public void setCurrentTierOrder(int v) { this.currentTierOrder = v; }
    public String getCurrentColorKey() { return currentColorKey; }
    public void setCurrentColorKey(String v) { this.currentColorKey = v; }
    public LocalDateTime getCurrentEarnedAt() { return currentEarnedAt; }
    public void setCurrentEarnedAt(LocalDateTime v) { this.currentEarnedAt = v; }
    public String getNextTierLabel() { return nextTierLabel; }
    public void setNextTierLabel(String v) { this.nextTierLabel = v; }
    public long getNextThreshold() { return nextThreshold; }
    public void setNextThreshold(long v) { this.nextThreshold = v; }
    public double getProgressToNext() { return progressToNext; }
    public void setProgressToNext(double v) { this.progressToNext = v; }
    public List<BadgeTierItemDTO> getTiers() { return tiers; }
    public void setTiers(List<BadgeTierItemDTO> v) { this.tiers = v; }
}
