package com.goaway.contexts.mood.api.dto;

public class SoupDTO {
    private String text;

    public SoupDTO() {}

    public SoupDTO(String text) {
        this.text = text;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
