package com.goaway.contexts.activity.api.dto;

import java.time.LocalDateTime;

/** 系列里的一档（5 档轨的一项）。 */
public class BadgeTierItemDTO {
    private String tier;       // LA/NPC/REN/TOP/HANG
    private String label;      // 拉/NPC/人上人/顶级/夯
    private String colorKey;
    private long threshold;
    private boolean earned;
    private LocalDateTime earnedAt;

    public BadgeTierItemDTO() {}

    public BadgeTierItemDTO(String tier, String label, String colorKey, long threshold,
                            boolean earned, LocalDateTime earnedAt) {
        this.tier = tier;
        this.label = label;
        this.colorKey = colorKey;
        this.threshold = threshold;
        this.earned = earned;
        this.earnedAt = earnedAt;
    }

    public String getTier() { return tier; }
    public String getLabel() { return label; }
    public String getColorKey() { return colorKey; }
    public long getThreshold() { return threshold; }
    public boolean isEarned() { return earned; }
    public LocalDateTime getEarnedAt() { return earnedAt; }
}
