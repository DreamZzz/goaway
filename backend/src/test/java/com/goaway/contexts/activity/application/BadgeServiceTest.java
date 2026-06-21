package com.goaway.contexts.activity.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goaway.contexts.activity.api.dto.BadgeAwardDTO;
import com.goaway.contexts.activity.api.dto.BadgeSeriesDTO;
import com.goaway.contexts.activity.api.dto.BadgeWallDTO;
import com.goaway.contexts.activity.application.rule.RuleEvaluator;
import com.goaway.contexts.activity.domain.ActivityType;
import com.goaway.contexts.activity.domain.UserBadge;
import com.goaway.contexts.activity.infrastructure.persistence.ActivityEventRepository;
import com.goaway.contexts.activity.infrastructure.persistence.BadgeDefinitionRepository;
import com.goaway.contexts.activity.infrastructure.persistence.UserBadgeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    private final ActivityEventRepository activityRepo = mock(ActivityEventRepository.class);
    private final UserBadgeRepository userBadgeRepo = mock(UserBadgeRepository.class);
    private final BadgeDefinitionRepository badgeDefRepo = mock(BadgeDefinitionRepository.class);
    private final BadgeService service = new BadgeService(activityRepo, userBadgeRepo, badgeDefRepo,
            new RuleEvaluator(), new ObjectMapper());

    private void waterCount(long n) {
        when(activityRepo.countByUserIdAndType(eq(1L), eq(ActivityType.WATER))).thenReturn(n);
    }

    @Test
    @DisplayName("喝水 60 次：跨过 拉+NPC，每系列只返回最高新档(NPC)，首次=非晋级")
    void awardCrossTiers() {
        waterCount(60);
        when(userBadgeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<BadgeAwardDTO> awards = service.evaluateAndAward(1L);

        // 落库了 拉 和 NPC 两档
        verify(userBadgeRepo).save(argThat(b -> ((UserBadge) b).getBadgeKey().equals("water.LA")));
        verify(userBadgeRepo).save(argThat(b -> ((UserBadge) b).getBadgeKey().equals("water.NPC")));
        // 但只返回最高新档用于弹窗
        BadgeAwardDTO water = awards.stream().filter(a -> a.getSeriesKey().equals("water")).findFirst().orElseThrow();
        assertEquals("NPC", water.getTier());
        assertFalse(water.isPromotion()); // 首次进系列
    }

    @Test
    @DisplayName("已持有 拉+NPC 再喝到 200：晋级到 人上人(promotion=true)")
    void promote() {
        waterCount(200);
        when(userBadgeRepo.findByUserId(1L)).thenReturn(List.of(
                new UserBadge(1L, "water.LA"), new UserBadge(1L, "water.NPC")));
        when(userBadgeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<BadgeAwardDTO> awards = service.evaluateAndAward(1L);

        BadgeAwardDTO water = awards.stream().filter(a -> a.getSeriesKey().equals("water")).findFirst().orElseThrow();
        assertEquals("REN", water.getTier());
        assertTrue(water.isPromotion());
    }

    @Test
    @DisplayName("已持有档位、未达更高档：不重复发放")
    void idempotent() {
        waterCount(60);
        when(userBadgeRepo.findByUserId(1L)).thenReturn(List.of(
                new UserBadge(1L, "water.LA"), new UserBadge(1L, "water.NPC")));

        List<BadgeAwardDTO> awards = service.evaluateAndAward(1L);

        assertTrue(awards.stream().noneMatch(a -> a.getSeriesKey().equals("water")));
        verify(userBadgeRepo, never()).save(any());
    }

    @Test
    @DisplayName("勋章墙：喝水系列当前档=NPC，下一档=人上人(200)，进度=60/200")
    void wall() {
        waterCount(60);
        when(userBadgeRepo.findByUserId(1L)).thenReturn(List.of(
                new UserBadge(1L, "water.LA"), new UserBadge(1L, "water.NPC")));

        BadgeWallDTO w = service.listWall(1L);
        BadgeSeriesDTO water = w.getSeries().stream().filter(s -> s.getSeriesKey().equals("water")).findFirst().orElseThrow();
        assertEquals("NPC", water.getCurrentTier());
        assertEquals("人上人", water.getNextTierLabel());
        assertEquals(200, water.getNextThreshold());
        assertEquals(0.3, water.getProgressToNext(), 0.001);
        assertEquals(5, water.getTiers().size());
    }
}
