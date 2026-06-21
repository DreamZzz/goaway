package com.goaway.contexts.activity.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 运营后台配置的榜单（内置榜见 LeaderboardQuery.Board，二者并存）。
 * scoreExprJson 为打分表达式（{@link com.goaway.contexts.activity.domain.rule.Expr}），
 * havingRuleJson 为可选过滤规则（{@link com.goaway.contexts.activity.domain.rule.Rule}）。
 * 均基于 activity_events。
 */
@Entity
@Table(name = "leaderboard_definitions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lb_def_key", columnNames = "board_key")
})
public class LeaderboardDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_key", nullable = false, length = 40)
    private String key;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(name = "score_expr_json", columnDefinition = "text", nullable = false)
    private String scoreExprJson;

    @Column(name = "having_rule_json", columnDefinition = "text")
    private String havingRuleJson;

    @Column(length = 16)
    private String unit = "count"; // seconds | count

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "period_default", length = 8)
    private String periodDefault = "week";

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

    public String getScoreExprJson() { return scoreExprJson; }
    public void setScoreExprJson(String scoreExprJson) { this.scoreExprJson = scoreExprJson; }

    public String getHavingRuleJson() { return havingRuleJson; }
    public void setHavingRuleJson(String havingRuleJson) { this.havingRuleJson = havingRuleJson; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public String getPeriodDefault() { return periodDefault; }
    public void setPeriodDefault(String periodDefault) { this.periodDefault = periodDefault; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
