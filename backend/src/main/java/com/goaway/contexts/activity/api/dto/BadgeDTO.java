package com.goaway.contexts.activity.api.dto;

import com.goaway.contexts.activity.domain.Badge;

import java.time.LocalDateTime;

/** 徽章展示项：目录元信息 + 当前进度 + 是否解锁。 */
public class BadgeDTO {
    private String key;
    private String title;
    private String description;
    private String kind;        // SINGLE | CUMULATIVE
    private String icon;        // 前端 Icon glyph 名
    private String unit;        // seconds | count
    private long threshold;
    private long current;
    private boolean earned;
    private LocalDateTime earnedAt;
    private double progress;     // 0..1

    public BadgeDTO() {}

    public BadgeDTO(String key, String title, String description, String kind, String icon,
                    String unit, long threshold, long current, boolean earned,
                    LocalDateTime earnedAt, double progress) {
        this.key = key;
        this.title = title;
        this.description = description;
        this.kind = kind;
        this.icon = icon;
        this.unit = unit;
        this.threshold = threshold;
        this.current = current;
        this.earned = earned;
        this.earnedAt = earnedAt;
        this.progress = progress;
    }

    public BadgeDTO(Badge badge, long current, boolean earned, LocalDateTime earnedAt, double progress) {
        this.key = badge.getKey();
        this.title = badge.getTitle();
        this.description = badge.getDescription();
        this.kind = badge.getKind().name();
        this.icon = badge.getIcon();
        this.unit = badge.getMetric().getUnit();
        this.threshold = badge.getThreshold();
        this.current = current;
        this.earned = earned;
        this.earnedAt = earnedAt;
        this.progress = progress;
    }

    public String getKey() { return key; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getKind() { return kind; }
    public String getIcon() { return icon; }
    public String getUnit() { return unit; }
    public long getThreshold() { return threshold; }
    public long getCurrent() { return current; }
    public boolean isEarned() { return earned; }
    public LocalDateTime getEarnedAt() { return earnedAt; }
    public double getProgress() { return progress; }
}
