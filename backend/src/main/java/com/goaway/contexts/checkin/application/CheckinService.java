package com.goaway.contexts.checkin.application;

import com.goaway.contexts.checkin.api.dto.CheckinSummaryDTO;
import com.goaway.contexts.checkin.domain.CheckinRecord;
import com.goaway.contexts.checkin.infrastructure.persistence.CheckinRecordRepository;
import com.goaway.shared.time.PeriodRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class CheckinService {

    private final CheckinRecordRepository repository;

    public CheckinService(CheckinRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * 今日打卡（幂等）。返回打卡后的汇总。
     */
    @Transactional
    public CheckinSummaryDTO checkin(Long userId) {
        LocalDate today = LocalDate.now();
        if (repository.findByUserIdAndCheckinDate(userId, today).isEmpty()) {
            int streak = repository.findTopByUserIdOrderByCheckinDateDesc(userId)
                    .filter(last -> last.getCheckinDate().equals(today.minusDays(1)))
                    .map(last -> last.getStreakCount() + 1)
                    .orElse(1);
            repository.save(new CheckinRecord(userId, today, streak));
        }
        return summary(userId);
    }

    @Transactional(readOnly = true)
    public CheckinSummaryDTO summary(Long userId) {
        LocalDate today = LocalDate.now();
        Optional<CheckinRecord> latest = repository.findTopByUserIdOrderByCheckinDateDesc(userId);

        boolean checkedInToday = latest.map(r -> r.getCheckinDate().equals(today)).orElse(false);
        // 连续天数仍然有效的条件：最近一次打卡是今天或昨天
        int currentStreak = latest
                .filter(r -> r.getCheckinDate().equals(today) || r.getCheckinDate().equals(today.minusDays(1)))
                .map(CheckinRecord::getStreakCount)
                .orElse(0);

        long totalDays = repository.countByUserId(userId);
        PeriodRange week = PeriodRange.of(PeriodRange.Period.WEEK, today);
        long thisWeekDays = repository.countByUserIdAndCheckinDateBetween(userId, week.start(), week.end());

        return new CheckinSummaryDTO(checkedInToday, currentStreak, totalDays, thisWeekDays);
    }
}
