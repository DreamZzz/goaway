package com.goaway.contexts.activity.api.dto;

import jakarta.validation.constraints.NotBlank;

public class RecordActivityRequest {

    /** WATER | SMOKE | POOP */
    @NotBlank
    private String type;

    /** 带薪拉屎时长（秒），其余类型可不传。 */
    private Integer durationSeconds;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
}
