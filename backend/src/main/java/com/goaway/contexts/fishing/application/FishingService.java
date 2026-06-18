package com.goaway.contexts.fishing.application;

import com.goaway.contexts.fishing.api.dto.FishingSummaryDTO;
import com.goaway.contexts.fishing.domain.FishingSession;
import com.goaway.contexts.fishing.infrastructure.persistence.FishingSessionRepository;
import com.goaway.shared.time.PeriodRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class FishingService {

    /** 单次上报上限：4 小时，过滤明显异常的批量补报。 */
    static final long MAX_REPORT_SECONDS = 4 * 3600;
    /** 单日累计上限：12 小时，宽松防刷。 */
    static final long MAX_DAILY_SECONDS = 12 * 3600;

    private final FishingSessionRepository repository;

    public FishingService(FishingSessionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public FishingSummaryDTO report(Long userId, long seconds) {
        long accepted = Math.min(Math.max(seconds, 0), MAX_REPORT_SECONDS);
        LocalDate today = LocalDate.now();
        FishingSession session = repository.findByUserIdAndSessionDate(userId, today)
                .orElseGet(() -> new FishingSession(userId, today, 0));
        session.setTotalSeconds(Math.min(session.getTotalSeconds() + accepted, MAX_DAILY_SECONDS));
        repository.save(session);
        return summary(userId);
    }

    @Transactional(readOnly = true)
    public FishingSummaryDTO summary(Long userId) {
        LocalDate today = LocalDate.now();
        long todaySeconds = repository.findByUserIdAndSessionDate(userId, today)
                .map(FishingSession::getTotalSeconds)
                .orElse(0L);
        PeriodRange week = PeriodRange.of(PeriodRange.Period.WEEK, today);
        long thisWeekSeconds = repository.sumSecondsByUserIdAndDateBetween(userId, week.start(), week.end());
        long totalSeconds = repository.sumTotalSecondsByUserId(userId);
        return new FishingSummaryDTO(todaySeconds, thisWeekSeconds, totalSeconds);
    }
}
