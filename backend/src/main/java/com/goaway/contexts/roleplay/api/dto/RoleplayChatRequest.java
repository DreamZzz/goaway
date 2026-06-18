package com.goaway.contexts.roleplay.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class RoleplayChatRequest {

    @NotBlank(message = "请选择角色")
    private String persona;

    /** 对话历史（含本轮用户最新消息），最后一条应为 user。 */
    @NotEmpty(message = "消息不能为空")
    @Size(max = 40, message = "对话过长")
    private List<Message> messages;

    public static class Message {
        @NotBlank
        private String role;     // user | assistant
        @NotBlank
        @Size(max = 1000)
        private String content;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public String getPersona() { return persona; }
    public void setPersona(String persona) { this.persona = persona; }

    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
}
