package com.goaway.contexts.leaderboard.api.dto;

/** 榜单列表项（内置 + 配置），供 App 动态渲染榜单选择条。 */
public class BoardInfoDTO {
    private String key;
    private String label;
    private String unit;          // seconds | count
    private String periodDefault; // day | week

    public BoardInfoDTO() {}

    public BoardInfoDTO(String key, String label, String unit, String periodDefault) {
        this.key = key;
        this.label = label;
        this.unit = unit;
        this.periodDefault = periodDefault;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getPeriodDefault() { return periodDefault; }
    public void setPeriodDefault(String periodDefault) { this.periodDefault = periodDefault; }
}
