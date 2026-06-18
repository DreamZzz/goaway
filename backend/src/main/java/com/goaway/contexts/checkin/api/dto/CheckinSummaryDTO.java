package com.goaway.contexts.checkin.api.dto;

public class CheckinSummaryDTO {
    private boolean checkedInToday;
    private int currentStreak;
    private long totalDays;
    private long thisWeekDays;

    public CheckinSummaryDTO() {}

    public CheckinSummaryDTO(boolean checkedInToday, int currentStreak, long totalDays, long thisWeekDays) {
        this.checkedInToday = checkedInToday;
        this.currentStreak = currentStreak;
        this.totalDays = totalDays;
        this.thisWeekDays = thisWeekDays;
    }

    public boolean isCheckedInToday() { return checkedInToday; }
    public void setCheckedInToday(boolean checkedInToday) { this.checkedInToday = checkedInToday; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public long getTotalDays() { return totalDays; }
    public void setTotalDays(long totalDays) { this.totalDays = totalDays; }

    public long getThisWeekDays() { return thisWeekDays; }
    public void setThisWeekDays(long thisWeekDays) { this.thisWeekDays = thisWeekDays; }
}
