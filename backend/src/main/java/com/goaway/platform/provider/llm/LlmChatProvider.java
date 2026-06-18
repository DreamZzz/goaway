package com.goaway.platform.provider.llm;

import com.goaway.platform.llm.LlmScene;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 通用 LLM 文本生成 provider，按场景（LlmScene）路由模型配置。
 * 流式为主，支持多轮 messages；非流式由流式累积得到。
 */
public interface LlmChatProvider {

    /**
     * 多轮流式生成，每段增量文本通过 onDelta 回调输出。
     */
    void streamChat(LlmScene scene, List<ChatMessage> messages, Consumer<String> onDelta);

    /**
     * 单轮便捷方法：system + user。
     */
    default void streamChat(LlmScene scene, String systemPrompt, String userPrompt, Consumer<String> onDelta) {
        List<ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(ChatMessage.system(systemPrompt));
        }
        messages.add(ChatMessage.user(userPrompt == null ? "" : userPrompt));
        streamChat(scene, messages, onDelta);
    }

    /**
     * 非流式生成，返回完整文本。
     */
    default String chat(LlmScene scene, String systemPrompt, String userPrompt) {
        StringBuilder sb = new StringBuilder();
        streamChat(scene, systemPrompt, userPrompt, sb::append);
        return sb.toString();
    }
}
