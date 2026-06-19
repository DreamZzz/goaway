package com.goaway.contexts.roleplay.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class RoleplayChatRequest {

    @NotBlank(message = "请选择角色")
    private String persona;

    /** 自定义角色描述（persona=custom 时使用，如「我的直属领导，阴阳怪气爱甩锅」）。 */
    @Size(max = 300)
    private String customPersona;

    /** 对话历史（含本轮用户最新消息），最后一条应为 user。 */
    @NotEmpty(message = "消息不能为空")
    @Size(max = 40, message = "对话过长")
    private List<RoleplayMessage> messages;

    public String getPersona() { return persona; }
    public void setPersona(String persona) { this.persona = persona; }

    public String getCustomPersona() { return customPersona; }
    public void setCustomPersona(String customPersona) { this.customPersona = customPersona; }

    public List<RoleplayMessage> getMessages() { return messages; }
    public void setMessages(List<RoleplayMessage> messages) { this.messages = messages; }
}
