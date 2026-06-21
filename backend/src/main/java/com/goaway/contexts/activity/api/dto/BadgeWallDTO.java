package com.goaway.contexts.activity.api.dto;

import java.util.List;

/** 勋章墙：内置系列（带档位/晋级）+ 配置勋章（单枚 extras）。 */
public class BadgeWallDTO {
    private List<BadgeSeriesDTO> series;
    private List<BadgeDTO> extras;

    public BadgeWallDTO() {}

    public BadgeWallDTO(List<BadgeSeriesDTO> series, List<BadgeDTO> extras) {
        this.series = series;
        this.extras = extras;
    }

    public List<BadgeSeriesDTO> getSeries() { return series; }
    public void setSeries(List<BadgeSeriesDTO> series) { this.series = series; }
    public List<BadgeDTO> getExtras() { return extras; }
    public void setExtras(List<BadgeDTO> extras) { this.extras = extras; }
}
