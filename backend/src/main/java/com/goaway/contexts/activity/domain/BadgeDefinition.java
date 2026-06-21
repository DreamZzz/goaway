package com.goaway.contexts.activity.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 运营后台配置的勋章（内置勋章见 {@link Badge} 枚举，二者并存）。
 * ruleJson 为 {@link com.goaway.contexts.activity.domain.rule.Rule} 的 JSON。
 */
@Entity
@Table(name = "badge_definitions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_badge_def_key", columnNames = "badge_key")
})
public class BadgeDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "badge_key", nullable = false, length = 40)
    private String key;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(length = 200)
    private String description;

    @Column(length = 40)
    private String icon;

    @Column(length = 16)
    private String kind; // SINGLE | CUMULATIVE

    @Column(name = "rule_json", columnDefinition = "text", nullable = false)
    private String ruleJson;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getRuleJson() { return ruleJson; }
    public void setRuleJson(String ruleJson) { this.ruleJson = ruleJson; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
