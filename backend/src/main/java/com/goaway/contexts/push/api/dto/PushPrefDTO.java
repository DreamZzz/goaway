package com.goaway.contexts.push.api.dto;

import com.goaway.contexts.push.domain.PushPreference;

public class PushPrefDTO {

    private boolean enabled;
    private String frequency;
    private Integer quietStart;
    private Integer quietEnd;

    public static PushPrefDTO from(PushPreference pref) {
        PushPrefDTO dto = new PushPrefDTO();
        dto.enabled = pref.isEnabled();
        dto.frequency = pref.getFrequency() == null ? null : pref.getFrequency().name();
        dto.quietStart = pref.getQuietStart();
        dto.quietEnd = pref.getQuietEnd();
        return dto;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public Integer getQuietStart() { return quietStart; }
    public void setQuietStart(Integer quietStart) { this.quietStart = quietStart; }

    public Integer getQuietEnd() { return quietEnd; }
    public void setQuietEnd(Integer quietEnd) { this.quietEnd = quietEnd; }
}
