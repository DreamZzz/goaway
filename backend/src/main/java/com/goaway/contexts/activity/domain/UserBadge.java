package com.goaway.contexts.activity.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 用户已解锁的徽章。badgeKey 对应 {@link Badge#getKey()}。每人每枚至多一条。
 */
@Entity
@Table(name = "user_badges", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_badge", columnNames = {"user_id", "badge_key"})
}, indexes = {
        @Index(name = "idx_user_badges_user", columnList = "user_id")
})
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "badge_key", nullable = false, length = 40)
    private String badgeKey;

    @CreationTimestamp
    @Column(name = "earned_at", updatable = false)
    private LocalDateTime earnedAt;

    public UserBadge() {}

    public UserBadge(Long userId, String badgeKey) {
        this.userId = userId;
        this.badgeKey = badgeKey;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getBadgeKey() { return badgeKey; }
    public LocalDateTime getEarnedAt() { return earnedAt; }
}
