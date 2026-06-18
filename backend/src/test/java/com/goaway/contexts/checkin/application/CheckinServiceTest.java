package com.goaway.contexts.checkin.application;

import com.goaway.contexts.checkin.api.dto.CheckinSummaryDTO;
import com.goaway.contexts.checkin.domain.CheckinRecord;
import com.goaway.contexts.checkin.infrastructure.persistence.CheckinRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckinServiceTest {

    private final CheckinRecordRepository repository = mock(CheckinRecordRepository.class);
    private final CheckinService service = new CheckinService(repository);

    @Test
    @DisplayName("首次打卡连续天数为 1 并写入今日记录")
    void firstCheckin_streakIsOne() {
        LocalDate today = LocalDate.now();
        when(repository.findByUserIdAndCheckinDate(1L, today)).thenReturn(Optional.empty());
        when(repository.findTopByUserIdOrderByCheckinDateDesc(1L)).thenReturn(Optional.empty());

        service.checkin(1L);

        ArgumentCaptor<CheckinRecord> captor = ArgumentCaptor.forClass(CheckinRecord.class);
        verify(repository).save(captor.capture());
        assertEquals(today, captor.getValue().getCheckinDate());
        assertEquals(1, captor.getValue().getStreakCount());
    }

    @Test
    @DisplayName("昨天打过卡，今天连续天数 +1")
    void consecutiveCheckin_incrementsStreak() {
        LocalDate today = LocalDate.now();
        CheckinRecord yesterday = new CheckinRecord(1L, today.minusDays(1), 3);
        when(repository.findByUserIdAndCheckinDate(1L, today)).thenReturn(Optional.empty());
        when(repository.findTopByUserIdOrderByCheckinDateDesc(1L)).thenReturn(Optional.of(yesterday));

        service.checkin(1L);

        ArgumentCaptor<CheckinRecord> captor = ArgumentCaptor.forClass(CheckinRecord.class);
        verify(repository).save(captor.capture());
        assertEquals(4, captor.getValue().getStreakCount());
    }

    @Test
    @DisplayName("断签后重新打卡，连续天数归 1")
    void brokenStreak_resetsToOne() {
        LocalDate today = LocalDate.now();
        CheckinRecord threeDaysAgo = new CheckinRecord(1L, today.minusDays(3), 5);
        when(repository.findByUserIdAndCheckinDate(1L, today)).thenReturn(Optional.empty());
        when(repository.findTopByUserIdOrderByCheckinDateDesc(1L)).thenReturn(Optional.of(threeDaysAgo));

        service.checkin(1L);

        ArgumentCaptor<CheckinRecord> captor = ArgumentCaptor.forClass(CheckinRecord.class);
        verify(repository).save(captor.capture());
        assertEquals(1, captor.getValue().getStreakCount());
    }

    @Test
    @DisplayName("当天重复打卡是幂等的，不再新增记录")
    void sameDayCheckin_isIdempotent() {
        LocalDate today = LocalDate.now();
        when(repository.findByUserIdAndCheckinDate(1L, today))
                .thenReturn(Optional.of(new CheckinRecord(1L, today, 2)));
        when(repository.findTopByUserIdOrderByCheckinDateDesc(1L))
                .thenReturn(Optional.of(new CheckinRecord(1L, today, 2)));

        CheckinSummaryDTO summary = service.checkin(1L);

        verify(repository, never()).save(any());
        assertTrue(summary.isCheckedInToday());
        assertEquals(2, summary.getCurrentStreak());
    }

    @Test
    @DisplayName("最近打卡早于昨天时，当前连续天数为 0")
    void summary_streakZero_whenLatestOlderThanYesterday() {
        LocalDate today = LocalDate.now();
        when(repository.findTopByUserIdOrderByCheckinDateDesc(1L))
                .thenReturn(Optional.of(new CheckinRecord(1L, today.minusDays(2), 9)));
        when(repository.countByUserId(1L)).thenReturn(9L);
        when(repository.countByUserIdAndCheckinDateBetween(anyLong(), any(), any())).thenReturn(0L);

        CheckinSummaryDTO summary = service.summary(1L);

        assertFalse(summary.isCheckedInToday());
        assertEquals(0, summary.getCurrentStreak());
        assertEquals(9L, summary.getTotalDays());
    }
}
