package com.goaway.contexts.roleplay.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 用户对 AI 生成内容（对线回复 / 毒舌推送）的举报记录。
 * 用于满足 App Store 1.2 对 AI / UGC 内容「可举报 + 开发者跟进」的要求。
 */
@Entity
@Table(name = "content_reports", indexes = {
        @Index(name = "idx_content_reports_created", columnList = "created_at")
})
public class ContentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 举报者用户 id（游客也有 id）；匿名为空。 */
    @Column(name = "user_id")
    private Long userId;

    /** 被举报的内容来源：roleplay / taunt 等。 */
    @Column(length = 24)
    private String source;

    @Column(length = 1000, nullable = false)
    private String content;

    @Column(length = 200)
    private String reason;

    @Column(name = "handled", nullable = false)
    private boolean handled = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public ContentReport() {}

    public ContentReport(Long userId, String source, String content, String reason) {
        this.userId = userId;
        this.source = source;
        this.content = content;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getSource() { return source; }
    public String getContent() { return content; }
    public String getReason() { return reason; }
    public boolean isHandled() { return handled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
