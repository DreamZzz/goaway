package com.goaway.contexts.weekly.application;

import com.goaway.platform.llm.LlmScene;
import com.goaway.platform.provider.llm.MockLlmChatProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class WeeklyServiceTest {

    @Test
    @DisplayName("currentWeekKey 输出 ISO 周次格式")
    void currentWeekKey_isoFormat() {
        // 2026-06-18 属于 2026 年第 25 周
        assertEquals("2026-W25", WeeklyService.currentWeekKey(LocalDate.of(2026, 6, 18)));
        // 跨年周归属上一年（ISO）
        assertTrue(WeeklyService.currentWeekKey(LocalDate.of(2026, 1, 1)).matches("\\d{4}-W\\d{2}"));
    }

    @Test
    @DisplayName("mock provider 基于碎片生成结构化周报")
    void mockProvider_buildsStructuredReport() {
        MockLlmChatProvider provider = new MockLlmChatProvider();
        StringBuilder sb = new StringBuilder();
        provider.streamChat(LlmScene.WEEKLY, WeeklyPrompts.SYSTEM,
                WeeklyPrompts.buildUserPrompt("完成了登录模块\n修复了排行榜 bug"), sb::append);
        String out = sb.toString();

        assertTrue(out.contains("本周工作完成情况"), "应包含完成情况小节");
        assertTrue(out.contains("下周计划"), "应包含下周计划小节");
        assertTrue(out.contains("登录模块"), "应包含输入碎片内容");
    }

    @Test
    @DisplayName("mock provider 流式分多段输出")
    void mockProvider_streamsMultipleSegments() {
        MockLlmChatProvider provider = new MockLlmChatProvider();
        int[] count = {0};
        provider.streamChat(LlmScene.WEEKLY, "", WeeklyPrompts.buildUserPrompt("做了A\n做了B"),
                delta -> count[0]++);
        assertTrue(count[0] > 1, "流式应分多段回调");
    }
}
