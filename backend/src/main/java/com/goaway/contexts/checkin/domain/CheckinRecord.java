package com.goaway.contexts.checkin.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每个用户每天至多一条打卡记录。streakCount 为写入当天时计算出的连续打卡天数。
 */
@Entity
@Table(
        name = "checkin_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_checkin_user_date", columnNames = {"user_id", "checkin_date"}),
        indexes = @Index(name = "idx_checkin_user_date", columnList = "user_id,checkin_date")
)
public class CheckinRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate;

    @Column(name = "streak_count", nullable = false)
    private int streakCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public CheckinRecord() {}

    public CheckinRecord(Long userId, LocalDate checkinDate, int streakCount) {
        this.userId = userId;
        this.checkinDate = checkinDate;
        this.streakCount = streakCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDate getCheckinDate() { return checkinDate; }
    public void setCheckinDate(LocalDate checkinDate) { this.checkinDate = checkinDate; }

    public int getStreakCount() { return streakCount; }
    public void setStreakCount(int streakCount) { this.streakCount = streakCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
