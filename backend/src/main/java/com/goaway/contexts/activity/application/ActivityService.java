package com.goaway.contexts.activity.application;

import com.goaway.contexts.activity.api.dto.ActivitySummaryDTO;
import com.goaway.contexts.activity.api.dto.BadgeAwardDTO;
import com.goaway.contexts.activity.api.dto.RecordActivityRequest;
import com.goaway.contexts.activity.domain.ActivityEvent;
import com.goaway.contexts.activity.domain.ActivityType;
import com.goaway.contexts.activity.infrastructure.persistence.ActivityEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActivityService {

    /** 单个时长事件上限：12 小时，过滤异常上报（摸鱼单次可达一个工作日）。 */
    private static final int MAX_DURATION_SECONDS = 12 * 3600;

    private final ActivityEventRepository repository;
    private final BadgeService badgeService;

    public ActivityService(ActivityEventRepository repository, BadgeService badgeService) {
        this.repository = repository;
        this.badgeService = badgeService;
    }

    /** 记录动作并返回本次新解锁的勋章档位（供客户端中奖弹窗）。 */
    @Transactional
    public List<BadgeAwardDTO> record(Long userId, RecordActivityRequest request) {
        ActivityType type;
        try {
            type = ActivityType.valueOf(request.getType().trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未知动作类型");
        }
        Integer duration = null;
        boolean timed = type == ActivityType.POOP || type == ActivityType.FISH;
        if (timed && request.getDurationSeconds() != null) {
            duration = Math.max(0, Math.min(request.getDurationSeconds(), MAX_DURATION_SECONDS));
        }
        repository.save(new ActivityEvent(userId, type, duration));
        return badgeService.evaluateAndAward(userId);
    }

    @Transactional(readOnly = true)
    public ActivitySummaryDTO todaySummary(Long userId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDateTime.now();
        long water = repository.countByUserIdAndTypeAndOccurredAtBetween(userId, ActivityType.WATER, start, end);
        long smoke = repository.countByUserIdAndTypeAndOccurredAtBetween(userId, ActivityType.SMOKE, start, end);
        long poopCount = repository.countByUserIdAndTypeAndOccurredAtBetween(userId, ActivityType.POOP, start, end);
        long poopSeconds = repository.sumDurationByUserIdAndTypeAndOccurredAtBetween(userId, ActivityType.POOP, start, end);
        return new ActivitySummaryDTO(water, smoke, poopCount, poopSeconds);
    }
}
