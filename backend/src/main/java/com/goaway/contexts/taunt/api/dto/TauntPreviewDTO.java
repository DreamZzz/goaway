package com.goaway.contexts.taunt.api.dto;

public class TauntPreviewDTO {

    private String content;
    private boolean sent;

    public TauntPreviewDTO(String content, boolean sent) {
        this.content = content;
        this.sent = sent;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }
}
