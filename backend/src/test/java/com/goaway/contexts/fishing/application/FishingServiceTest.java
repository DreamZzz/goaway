package com.goaway.contexts.fishing.application;

import com.goaway.contexts.fishing.domain.FishingSession;
import com.goaway.contexts.fishing.infrastructure.persistence.FishingSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FishingServiceTest {

    private final FishingSessionRepository repository = mock(FishingSessionRepository.class);
    private final FishingService service = new FishingService(repository);

    @Test
    @DisplayName("单次上报超过 4 小时会被截断")
    void singleReport_cappedAtMaxReport() {
        LocalDate today = LocalDate.now();
        when(repository.findByUserIdAndSessionDate(1L, today)).thenReturn(Optional.empty());

        service.report(1L, FishingService.MAX_REPORT_SECONDS + 10_000);

        ArgumentCaptor<FishingSession> captor = ArgumentCaptor.forClass(FishingSession.class);
        verify(repository).save(captor.capture());
        assertEquals(FishingService.MAX_REPORT_SECONDS, captor.getValue().getTotalSeconds());
    }

    @Test
    @DisplayName("当天累计不超过每日上限")
    void dailyTotal_cappedAtMaxDaily() {
        LocalDate today = LocalDate.now();
        FishingSession existing = new FishingSession(1L, today, FishingService.MAX_DAILY_SECONDS - 100);
        when(repository.findByUserIdAndSessionDate(1L, today)).thenReturn(Optional.of(existing));

        service.report(1L, FishingService.MAX_REPORT_SECONDS);

        ArgumentCaptor<FishingSession> captor = ArgumentCaptor.forClass(FishingSession.class);
        verify(repository).save(captor.capture());
        assertEquals(FishingService.MAX_DAILY_SECONDS, captor.getValue().getTotalSeconds());
    }

    @Test
    @DisplayName("正常累加当天摸鱼秒数")
    void report_accumulatesNormally() {
        LocalDate today = LocalDate.now();
        when(repository.findByUserIdAndSessionDate(1L, today))
                .thenReturn(Optional.of(new FishingSession(1L, today, 600)));

        service.report(1L, 300);

        ArgumentCaptor<FishingSession> captor = ArgumentCaptor.forClass(FishingSession.class);
        verify(repository).save(captor.capture());
        assertEquals(900, captor.getValue().getTotalSeconds());
    }

    @Test
    @DisplayName("汇总返回今日/本周/累计秒数")
    void summary_returnsAggregates() {
        LocalDate today = LocalDate.now();
        when(repository.findByUserIdAndSessionDate(eq(1L), eq(today)))
                .thenReturn(Optional.of(new FishingSession(1L, today, 1200)));
        when(repository.sumSecondsByUserIdAndDateBetween(eq(1L), any(), any())).thenReturn(5000L);
        when(repository.sumTotalSecondsByUserId(1L)).thenReturn(99999L);

        var summary = service.summary(1L);

        assertEquals(1200, summary.getTodaySeconds());
        assertEquals(5000, summary.getThisWeekSeconds());
        assertEquals(99999, summary.getTotalSeconds());
    }
}
