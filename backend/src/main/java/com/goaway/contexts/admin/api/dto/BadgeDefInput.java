package com.goaway.contexts.admin.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

/** 后台新建/编辑配置勋章的请求体。rule 为前端构建的规则对象（Rule 结构）。 */
public class BadgeDefInput {
    private String key;
    private String title;
    private String description;
    private String icon;
    private String kind;       // SINGLE | CUMULATIVE
    private JsonNode rule;      // 规则对象
    private Boolean enabled;
    private Integer sortOrder;

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
    public JsonNode getRule() { return rule; }
    public void setRule(JsonNode rule) { this.rule = rule; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
