package com.goaway.contexts.fishing.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class FishingReportRequest {

    /** 本次新增的摸鱼秒数（自上次上报以来）。 */
    @NotNull
    @Min(value = 1, message = "上报秒数必须为正")
    private Long seconds;

    public Long getSeconds() { return seconds; }
    public void setSeconds(Long seconds) { this.seconds = seconds; }
}
