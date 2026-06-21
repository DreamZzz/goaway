package com.goaway.contexts.activity.api.dto;

/** 今日动作汇总，字段与前端本地计数结构一致，便于登录后直接替换数据源。 */
public class ActivitySummaryDTO {
    private long water;
    private long smoke;
    private long poopCount;
    private long poopSeconds;

    public ActivitySummaryDTO() {}

    public ActivitySummaryDTO(long water, long smoke, long poopCount, long poopSeconds) {
        this.water = water;
        this.smoke = smoke;
        this.poopCount = poopCount;
        this.poopSeconds = poopSeconds;
    }

    public long getWater() { return water; }
    public void setWater(long water) { this.water = water; }

    public long getSmoke() { return smoke; }
    public void setSmoke(long smoke) { this.smoke = smoke; }

    public long getPoopCount() { return poopCount; }
    public void setPoopCount(long poopCount) { this.poopCount = poopCount; }

    public long getPoopSeconds() { return poopSeconds; }
    public void setPoopSeconds(long poopSeconds) { this.poopSeconds = poopSeconds; }
}
