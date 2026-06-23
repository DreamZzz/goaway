package com.goaway.contexts.taunt.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 一条毒舌推送的发送记录：用于频控（每日上限/间隔）、去重与历史回溯。
 */
@Entity
@Table(name = "taunt_logs", indexes = {
        @Index(name = "idx_taunt_logs_user_time", columnList = "user_id, sent_at")
})
public class TauntLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", length = 24, nullable = false)
    private TauntTrigger trigger;

    @Column(length = 200, nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 200)
    private String detail;

    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;

    public TauntLog() {}

    public TauntLog(Long userId, TauntTrigger trigger, String content, boolean success, String detail) {
        this.userId = userId;
        this.trigger = trigger;
        this.content = content;
        this.success = success;
        this.detail = detail;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public TauntTrigger getTrigger() { return trigger; }
    public String getContent() { return content; }
    public boolean isSuccess() { return success; }
    public String getDetail() { return detail; }
    public LocalDateTime getSentAt() { return sentAt; }
}
