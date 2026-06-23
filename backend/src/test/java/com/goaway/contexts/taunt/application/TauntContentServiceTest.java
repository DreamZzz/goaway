package com.goaway.contexts.taunt.application;

import com.goaway.contexts.taunt.domain.TauntTrigger;
import com.goaway.contexts.workprofile.domain.WorkProfile;
import com.goaway.contexts.workprofile.infrastructure.persistence.WorkProfileRepository;
import com.goaway.platform.llm.LlmScene;
import com.goaway.platform.provider.llm.LlmChatProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TauntContentServiceTest {

    private final WorkProfileRepository workProfileRepository = mock(WorkProfileRepository.class);
    private final LlmChatProvider llm = mock(LlmChatProvider.class);
    private final TauntContentService service = new TauntContentService(workProfileRepository, llm);

    private WorkProfile profileWithHated() {
        WorkProfile p = new WorkProfile(1L);
        p.setHatedRelation("领导");
        p.setHatedNickname("画饼王");
        p.setHatedTraits("张口闭口格局，周末发需求");
        return p;
    }

    @Test
    @DisplayName("有最讨厌的人画像时，system prompt 注入其特征")
    void usesHatedPersonProfile() {
        when(workProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profileWithHated()));
        when(llm.chat(eq(LlmScene.GENERAL), any(), any())).thenReturn("周末了？需求给你排满了。");

        String content = service.generate(1L, TauntTrigger.SCHEDULED);

        assertEquals("周末了？需求给你排满了。", content);
        ArgumentCaptor<String> system = ArgumentCaptor.forClass(String.class);
        verify(llm).chat(eq(LlmScene.GENERAL), system.capture(), any());
        assertTrue(system.getValue().contains("画饼王"));
        assertTrue(system.getValue().contains("张口闭口格局"));
    }

    @Test
    @DisplayName("LLM 抛异常时回退到触发自带兜底文案")
    void fallsBackWhenLlmThrows() {
        when(workProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profileWithHated()));
        when(llm.chat(any(), any(), any())).thenThrow(new RuntimeException("llm down"));

        String content = service.generate(1L, TauntTrigger.RECALL);

        assertEquals(TauntTrigger.RECALL.fallbackLine(), content);
    }

    @Test
    @DisplayName("LLM 返回空白时回退兜底")
    void fallsBackWhenBlank() {
        when(workProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profileWithHated()));
        when(llm.chat(any(), any(), any())).thenReturn("   ");

        String content = service.generate(1L, TauntTrigger.SCHEDULED);

        assertEquals(TauntTrigger.SCHEDULED.fallbackLine(), content);
    }

    @Test
    @DisplayName("没填画像也能生成：用预设角色，仍调用 LLM")
    void noProfileUsesPreset() {
        when(workProfileRepository.findByUserId(7L)).thenReturn(Optional.empty());
        when(llm.chat(eq(LlmScene.GENERAL), any(), any())).thenReturn("又在划水？");

        String content = service.generate(7L, TauntTrigger.SCHEDULED);

        assertEquals("又在划水？", content);
        verify(llm).chat(eq(LlmScene.GENERAL), any(), any());
    }

    @Test
    @DisplayName("清洗：去掉成对引号并截断超长")
    void sanitizesQuotesAndLength() {
        when(workProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(llm.chat(any(), any(), any())).thenReturn("“你好啊打工人”");

        String content = service.generate(1L, TauntTrigger.SCHEDULED);
        assertEquals("你好啊打工人", content);

        when(llm.chat(any(), any(), any())).thenReturn("毒".repeat(100));
        String capped = service.generate(1L, TauntTrigger.SCHEDULED);
        assertTrue(capped.length() <= 60);
    }
}
