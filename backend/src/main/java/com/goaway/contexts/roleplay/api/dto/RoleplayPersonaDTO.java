package com.goaway.contexts.roleplay.api.dto;

import com.goaway.contexts.roleplay.domain.RoleplayPersona;

public class RoleplayPersonaDTO {
    private String code;
    private String name;
    private String emoji;
    private String description;

    public static RoleplayPersonaDTO from(RoleplayPersona p) {
        RoleplayPersonaDTO dto = new RoleplayPersonaDTO();
        dto.code = p.code();
        dto.name = p.displayName();
        dto.emoji = p.emoji();
        dto.description = p.description();
        return dto;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
