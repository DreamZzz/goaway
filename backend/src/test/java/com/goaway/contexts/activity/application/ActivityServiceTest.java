package com.goaway.contexts.activity.application;

import com.goaway.contexts.activity.api.dto.ActivitySummaryDTO;
import com.goaway.contexts.activity.api.dto.RecordActivityRequest;
import com.goaway.contexts.activity.domain.ActivityEvent;
import com.goaway.contexts.activity.domain.ActivityType;
import com.goaway.contexts.activity.infrastructure.persistence.ActivityEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    private final ActivityEventRepository repo = mock(ActivityEventRepository.class);
    private final BadgeService badgeService = mock(BadgeService.class);
    private final ActivityService service = new ActivityService(repo, badgeService);

    private RecordActivityRequest req(String type, Integer dur) {
        RecordActivityRequest r = new RecordActivityRequest();
        r.setType(type); r.setDurationSeconds(dur);
        return r;
    }

    @Test
    @DisplayName("喝水事件落库，type=WATER 且无时长")
    void recordWater() {
        service.record(1L, req("WATER", null));
        ArgumentCaptor<ActivityEvent> c = ArgumentCaptor.forClass(ActivityEvent.class);
        verify(repo).save(c.capture());
        assertEquals(ActivityType.WATER, c.getValue().getType());
        assertNull(c.getValue().getDurationSeconds());
    }

    @Test
    @DisplayName("拉屎事件记录时长，超 12 小时截断")
    void recordPoopCapped() {
        service.record(1L, req("poop", 99999));
        ArgumentCaptor<ActivityEvent> c = ArgumentCaptor.forClass(ActivityEvent.class);
        verify(repo).save(c.capture());
        assertEquals(ActivityType.POOP, c.getValue().getType());
        assertEquals(12 * 3600, c.getValue().getDurationSeconds());
    }

    @Test
    @DisplayName("摸鱼单次事件记录时长")
    void recordFish() {
        service.record(1L, req("FISH", 5400));
        ArgumentCaptor<ActivityEvent> c = ArgumentCaptor.forClass(ActivityEvent.class);
        verify(repo).save(c.capture());
        assertEquals(ActivityType.FISH, c.getValue().getType());
        assertEquals(5400, c.getValue().getDurationSeconds());
    }

    @Test
    @DisplayName("未知类型抛 400")
    void recordUnknownType() {
        assertThrows(ResponseStatusException.class, () -> service.record(1L, req("DANCE", null)));
    }

    @Test
    @DisplayName("今日汇总聚合各类型计数与拉屎时长")
    void todaySummary() {
        when(repo.countByUserIdAndTypeAndOccurredAtBetween(eq(1L), eq(ActivityType.WATER), any(), any())).thenReturn(5L);
        when(repo.countByUserIdAndTypeAndOccurredAtBetween(eq(1L), eq(ActivityType.SMOKE), any(), any())).thenReturn(3L);
        when(repo.countByUserIdAndTypeAndOccurredAtBetween(eq(1L), eq(ActivityType.POOP), any(), any())).thenReturn(2L);
        when(repo.sumDurationByUserIdAndTypeAndOccurredAtBetween(eq(1L), eq(ActivityType.POOP), any(), any())).thenReturn(900L);

        ActivitySummaryDTO s = service.todaySummary(1L);
        assertEquals(5, s.getWater());
        assertEquals(3, s.getSmoke());
        assertEquals(2, s.getPoopCount());
        assertEquals(900, s.getPoopSeconds());
    }
}
