package com.goaway.contexts.taunt.api.dto;

import com.goaway.contexts.taunt.domain.TauntLog;

import java.time.LocalDateTime;

/**
 * 毒舌收件箱条目：客户端按 id 游标增量同步，落到本地「最讨厌的人」会话。
 */
public class TauntInboxItemDTO {

    private Long id;
    private String content;
    private String trigger;
    private LocalDateTime sentAt;

    public static TauntInboxItemDTO from(TauntLog log) {
        TauntInboxItemDTO dto = new TauntInboxItemDTO();
        dto.id = log.getId();
        dto.content = log.getContent();
        dto.trigger = log.getTrigger() == null ? null : log.getTrigger().name();
        dto.sentAt = log.getSentAt();
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTrigger() { return trigger; }
    public void setTrigger(String trigger) { this.trigger = trigger; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
