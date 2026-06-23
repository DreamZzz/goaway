package com.goaway.contexts.push.domain;

/**
 * 用户可调的毒舌推送频率档：决定两条之间的最小间隔与每日上限。
 * OFF 表示关闭主动推送。
 */
public enum PushFrequency {
    OFF(0, 0),
    LOW(720, 1),      // 低频：约半天一条，每日至多 1
    NORMAL(240, 3),   // 正常：约 4 小时一条，每日至多 3
    HIGH(60, 6);      // 狂轰：约 1 小时一条，每日至多 6

    private final int intervalMinutes;
    private final int dailyCap;

    PushFrequency(int intervalMinutes, int dailyCap) {
        this.intervalMinutes = intervalMinutes;
        this.dailyCap = dailyCap;
    }

    public int intervalMinutes() { return intervalMinutes; }
    public int dailyCap() { return dailyCap; }
    public boolean isOff() { return this == OFF; }

    public static PushFrequency fromString(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
