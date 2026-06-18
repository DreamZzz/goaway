package com.goaway.contexts.fishing.api.dto;

public class FishingSummaryDTO {
    private long todaySeconds;
    private long thisWeekSeconds;
    private long totalSeconds;

    public FishingSummaryDTO() {}

    public FishingSummaryDTO(long todaySeconds, long thisWeekSeconds, long totalSeconds) {
        this.todaySeconds = todaySeconds;
        this.thisWeekSeconds = thisWeekSeconds;
        this.totalSeconds = totalSeconds;
    }

    public long getTodaySeconds() { return todaySeconds; }
    public void setTodaySeconds(long todaySeconds) { this.todaySeconds = todaySeconds; }

    public long getThisWeekSeconds() { return thisWeekSeconds; }
    public void setThisWeekSeconds(long thisWeekSeconds) { this.thisWeekSeconds = thisWeekSeconds; }

    public long getTotalSeconds() { return totalSeconds; }
    public void setTotalSeconds(long totalSeconds) { this.totalSeconds = totalSeconds; }
}
