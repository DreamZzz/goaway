package com.goaway.contexts.weekly.api.dto;

import com.goaway.contexts.weekly.domain.WeeklyReport;

import java.time.LocalDateTime;

public class WeeklyReportDTO {
    private Long id;
    private String weekKey;
    private String inputText;
    private String content;
    private String status;
    private LocalDateTime createdAt;

    public static WeeklyReportDTO from(WeeklyReport r) {
        WeeklyReportDTO dto = new WeeklyReportDTO();
        dto.id = r.getId();
        dto.weekKey = r.getWeekKey();
        dto.inputText = r.getInputText();
        dto.content = r.getContent();
        dto.status = r.getStatus() == null ? null : r.getStatus().name();
        dto.createdAt = r.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWeekKey() { return weekKey; }
    public void setWeekKey(String weekKey) { this.weekKey = weekKey; }

    public String getInputText() { return inputText; }
    public void setInputText(String inputText) { this.inputText = inputText; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
