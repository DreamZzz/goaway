package com.goaway.contexts.admin.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

/** 后台新建/编辑配置榜的请求体。scoreExpr 为打分表达式对象，having 为可选过滤规则对象。 */
public class LeaderboardDefInput {
    private String key;
    private String title;
    private JsonNode scoreExpr;
    private JsonNode having;     // 可空
    private String unit;         // seconds | count
    private Boolean enabled;
    private Integer sortOrder;
    private String periodDefault; // day | week

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public JsonNode getScoreExpr() { return scoreExpr; }
    public void setScoreExpr(JsonNode scoreExpr) { this.scoreExpr = scoreExpr; }
    public JsonNode getHaving() { return having; }
    public void setHaving(JsonNode having) { this.having = having; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getPeriodDefault() { return periodDefault; }
    public void setPeriodDefault(String periodDefault) { this.periodDefault = periodDefault; }
}
