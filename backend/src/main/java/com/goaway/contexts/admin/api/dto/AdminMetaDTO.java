package com.goaway.contexts.admin.api.dto;

import java.util.List;

/** 后台元信息：可用指标 + 运算符白名单，供可视化规则构建器下拉用。 */
public class AdminMetaDTO {

    public static class MetricInfo {
        public String key;
        public String label;
        public String unit;
        public MetricInfo(String key, String label, String unit) {
            this.key = key; this.label = label; this.unit = unit;
        }
    }

    private List<MetricInfo> metrics;
    private List<String> arithOps;
    private List<String> compareOps;
    private List<String> logicOps;
    private List<String> badgeKinds;
    private List<String> icons;

    public List<MetricInfo> getMetrics() { return metrics; }
    public void setMetrics(List<MetricInfo> metrics) { this.metrics = metrics; }
    public List<String> getArithOps() { return arithOps; }
    public void setArithOps(List<String> arithOps) { this.arithOps = arithOps; }
    public List<String> getCompareOps() { return compareOps; }
    public void setCompareOps(List<String> compareOps) { this.compareOps = compareOps; }
    public List<String> getLogicOps() { return logicOps; }
    public void setLogicOps(List<String> logicOps) { this.logicOps = logicOps; }
    public List<String> getBadgeKinds() { return badgeKinds; }
    public void setBadgeKinds(List<String> badgeKinds) { this.badgeKinds = badgeKinds; }
    public List<String> getIcons() { return icons; }
    public void setIcons(List<String> icons) { this.icons = icons; }
}
