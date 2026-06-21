package com.goaway.contexts.activity.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 一条离散动作事件 = 一次发生。作为榜单分析的基础数据：
 * 谁(userId) 在何时(occurredAt) 做了什么(type)，带薪拉屎额外记时长(durationSeconds)。
 */
@Entity
@Table(name = "activity_events", indexes = {
        @Index(name = "idx_activity_user_type_time", columnList = "user_id,type,occurred_at"),
        @Index(name = "idx_activity_time", columnList = "occurred_at")
})
public class ActivityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ActivityType type;

    /** 带薪拉屎的时长（秒）；喝水/抽烟为 null。 */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @CreationTimestamp
    @Column(name = "occurred_at", updatable = false)
    private LocalDateTime occurredAt;

    public ActivityEvent() {}

    public ActivityEvent(Long userId, ActivityType type, Integer durationSeconds) {
        this.userId = userId;
        this.type = type;
        this.durationSeconds = durationSeconds;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public ActivityType getType() { return type; }
    public void setType(ActivityType type) { this.type = type; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
