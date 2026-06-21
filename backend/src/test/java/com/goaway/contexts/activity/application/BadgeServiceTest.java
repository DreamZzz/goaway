package com.goaway.contexts.activity.application;

import com.goaway.contexts.activity.api.dto.BadgeDTO;
import com.goaway.contexts.activity.domain.ActivityType;
import com.goaway.contexts.activity.infrastructure.persistence.ActivityEventRepository;
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
    private final com.goaway.contexts.activity.infrastructure.persistence.BadgeDefinitionRepository badgeDefRepo =
            mock(com.goaway.contexts.activity.infrastructure.persistence.BadgeDefinitionRepository.class);
    private final BadgeService service = new BadgeService(activityRepo, userBadgeRepo, badgeDefRepo,
            new com.goaway.contexts.activity.application.rule.RuleEvaluator(),
            new com.fasterxml.jackson.databind.ObjectMapper());

    @Test
    @DisplayName("单次摸鱼满 1 小时解锁「摸鱼马拉松」，未达 4 小时不解锁「带薪一整天」")
    void awardSingleFishMarathon() {
        when(activityRepo.maxDurationByUserIdAndType(eq(1L), eq(ActivityType.FISH))).thenReturn(3700L);
        // 其余指标默认 0；徽章均未持有
        when(userBadgeRepo.existsByUserIdAndBadgeKey(eq(1L), any())).thenReturn(false);

        List<String> newly = service.evaluateAndAward(1L);

        assertTrue(newly.contains("FISH_MARATHON"));
        assertFalse(newly.contains("FISH_ALLDAY"));
        verify(userBadgeRepo, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("已持有的徽章不重复发放")
    void idempotentAward() {
        when(activityRepo.maxDurationByUserIdAndType(eq(1L), eq(ActivityType.FISH))).thenReturn(3700L);
        when(userBadgeRepo.existsByUserIdAndBadgeKey(eq(1L), eq("FISH_MARATHON"))).thenReturn(true);

        List<String> newly = service.evaluateAndAward(1L);

        assertFalse(newly.contains("FISH_MARATHON"));
    }

    @Test
    @DisplayName("列表包含全部目录徽章并带进度")
    void listBadgesProgress() {
        when(activityRepo.countByUserIdAndType(eq(1L), eq(ActivityType.WATER))).thenReturn(50L);
        when(userBadgeRepo.findByUserId(1L)).thenReturn(List.of());

        List<BadgeDTO> badges = service.listBadges(1L);

        assertEquals(8, badges.size());
        BadgeDTO water = badges.stream().filter(b -> b.getKey().equals("WATER_100")).findFirst().orElseThrow();
        assertEquals(0.5, water.getProgress(), 0.001); // 50 / 100
        assertFalse(water.isEarned());
    }
}
