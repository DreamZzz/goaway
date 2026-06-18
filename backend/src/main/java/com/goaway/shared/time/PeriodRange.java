package com.goaway.shared.time;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * 统一的「日 / 周」周期区间计算，供打卡、摸鱼、排行榜复用。
 * 周以周一为起点（ISO）。区间为闭区间 [start, end]。
 */
public record PeriodRange(LocalDate start, LocalDate end) {

    public enum Period { DAY, WEEK }

    public static PeriodRange of(Period period, LocalDate today) {
        return switch (period) {
            case DAY -> new PeriodRange(today, today);
            case WEEK -> new PeriodRange(
                    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
        };
    }

    public static Period parsePeriod(String raw) {
        if (raw != null && raw.equalsIgnoreCase("week")) {
            return Period.WEEK;
        }
        return Period.DAY;
    }
}
