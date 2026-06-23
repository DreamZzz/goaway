package com.goaway.contexts.push.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户的毒舌推送偏好与活跃水位（每个用户一条）。
 * 频率/免打扰决定「该不该发」；lastTauntAt 做频控、lastActiveAt 做不活跃召回。
 */
@Entity
@Table(name = "push_preferences")
public class PushPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", length = 16, nullable = false)
    private PushFrequency frequency = PushFrequency.NORMAL;

    /** 免打扰起止（小时，0-23）；start==end 表示不设免打扰。默认夜间 22-8 静音。 */
    @Column(name = "quiet_start")
    private Integer quietStart = 22;

    @Column(name = "quiet_end")
    private Integer quietEnd = 8;

    @Column(name = "last_taunt_at")
    private LocalDateTime lastTauntAt;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public PushPreference() {}

    public PushPreference(Long userId) {
        this.userId = userId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public PushFrequency getFrequency() { return frequency; }
    public void setFrequency(PushFrequency frequency) { this.frequency = frequency; }

    public Integer getQuietStart() { return quietStart; }
    public void setQuietStart(Integer quietStart) { this.quietStart = quietStart; }

    public Integer getQuietEnd() { return quietEnd; }
    public void setQuietEnd(Integer quietEnd) { this.quietEnd = quietEnd; }

    public LocalDateTime getLastTauntAt() { return lastTauntAt; }
    public void setLastTauntAt(LocalDateTime lastTauntAt) { this.lastTauntAt = lastTauntAt; }

    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /** 是否处于免打扰时段（含跨午夜，如 22→8）。 */
    public boolean isQuietHour(int hour) {
        if (quietStart == null || quietEnd == null || quietStart.equals(quietEnd)) {
            return false;
        }
        if (quietStart < quietEnd) {
            return hour >= quietStart && hour < quietEnd;
        }
        return hour >= quietStart || hour < quietEnd;
    }
}
