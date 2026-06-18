package com.goaway.contexts.roleplay;

import com.goaway.contexts.roleplay.domain.RoleplayPersona;
import com.goaway.platform.llm.LlmScene;
import com.goaway.platform.provider.llm.ChatMessage;
import com.goaway.platform.provider.llm.MockLlmChatProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoleplayTest {

    @Test
    @DisplayName("persona fromCode 解析与 systemPrompt 含角色与安全约束")
    void persona_lookupAndPrompt() {
        RoleplayPersona boss = RoleplayPersona.fromCode("hustle_boss").orElseThrow();
        assertEquals("暴躁老板", boss.displayName());
        assertTrue(boss.systemPrompt().contains("老板"));
        assertTrue(boss.systemPrompt().contains("禁止辱骂"), "应包含安全约束");
        assertTrue(RoleplayPersona.fromCode("not_exist").isEmpty());
    }

    @Test
    @DisplayName("mock provider 在 GENERAL 场景返回角色化回应（非周报）")
    void mockProvider_generalRoleplayReply() {
        MockLlmChatProvider provider = new MockLlmChatProvider();
        StringBuilder sb = new StringBuilder();
        List<ChatMessage> messages = List.of(
                ChatMessage.system(RoleplayPersona.HUSTLE_BOSS.systemPrompt()),
                ChatMessage.user("凭什么又让我加班？")
        );
        provider.streamChat(LlmScene.GENERAL, messages, sb::append);
        String out = sb.toString();

        assertFalse(out.contains("本周工作周报"), "GENERAL 场景不应生成周报");
        assertTrue(out.contains("老板"), "应体现角色");
    }

    @Test
    @DisplayName("mock provider 多轮 messages 取最后一条 user")
    void mockProvider_usesLastUserMessage() {
        MockLlmChatProvider provider = new MockLlmChatProvider();
        StringBuilder sb = new StringBuilder();
        List<ChatMessage> messages = List.of(
                ChatMessage.system(RoleplayPersona.PASSIVE_COLLEAGUE.systemPrompt()),
                ChatMessage.user("第一句"),
                ChatMessage.assistant("哦？"),
                ChatMessage.user("你少阴阳怪气")
        );
        provider.streamChat(LlmScene.GENERAL, messages, sb::append);
        assertTrue(sb.toString().contains("同事"));
    }
}
