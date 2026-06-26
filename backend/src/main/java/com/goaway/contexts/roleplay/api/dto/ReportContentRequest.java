package com.goaway.contexts.roleplay.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReportContentRequest {

    @NotBlank(message = "举报内容不能为空")
    @Size(max = 1000)
    private String content;

    @Size(max = 200)
    private String reason;

    @Size(max = 24)
    private String source;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
