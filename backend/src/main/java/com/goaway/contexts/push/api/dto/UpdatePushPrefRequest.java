package com.goaway.contexts.push.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class UpdatePushPrefRequest {

    private Boolean enabled;

    /** OFF / LOW / NORMAL / HIGH，大小写不敏感。 */
    private String frequency;

    @Min(0) @Max(23)
    private Integer quietStart;

    @Min(0) @Max(23)
    private Integer quietEnd;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public Integer getQuietStart() { return quietStart; }
    public void setQuietStart(Integer quietStart) { this.quietStart = quietStart; }

    public Integer getQuietEnd() { return quietEnd; }
    public void setQuietEnd(Integer quietEnd) { this.quietEnd = quietEnd; }
}
