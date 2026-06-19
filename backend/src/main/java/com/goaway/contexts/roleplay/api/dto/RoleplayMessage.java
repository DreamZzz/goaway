package com.goaway.contexts.roleplay.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 一条对话消息（顶层 DTO，便于契约生成器映射）。role 取 user | assistant。
 */
public class RoleplayMessage {

    @NotBlank
    private String role;

    @NotBlank
    @Size(max = 1000)
    private String content;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
