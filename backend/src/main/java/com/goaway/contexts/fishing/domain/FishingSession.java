package com.goaway.contexts.fishing.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每个用户每天一条摸鱼记录，累计当天上报的摸鱼秒数。
 */
@Entity
@Table(
        name = "fishing_sessions",
        uniqueConstraints = @UniqueConstraint(name = "uk_fishing_user_date", columnNames = {"user_id", "session_date"}),
        indexes = @Index(name = "idx_fishing_user_date", columnList = "user_id,session_date")
)
public class FishingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "total_seconds", nullable = false)
    private long totalSeconds;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public FishingSession() {}

    public FishingSession(Long userId, LocalDate sessionDate, long totalSeconds) {
        this.userId = userId;
        this.sessionDate = sessionDate;
        this.totalSeconds = totalSeconds;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }

    public long getTotalSeconds() { return totalSeconds; }
    public void setTotalSeconds(long totalSeconds) { this.totalSeconds = totalSeconds; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
