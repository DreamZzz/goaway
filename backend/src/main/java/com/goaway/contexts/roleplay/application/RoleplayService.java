package com.goaway.contexts.roleplay.application;

import com.goaway.contexts.roleplay.api.dto.RoleplayChatRequest;
import com.goaway.contexts.roleplay.api.dto.RoleplayMessage;
import com.goaway.contexts.roleplay.domain.RoleplayPersona;
import com.goaway.platform.llm.LlmScene;
import com.goaway.platform.provider.llm.ChatMessage;
import com.goaway.platform.provider.llm.LlmChatProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class RoleplayService {

    private static final Logger log = LoggerFactory.getLogger(RoleplayService.class);
    private static final long SSE_TIMEOUT_MS = 90_000;

    private final LlmChatProvider llmChatProvider;

    public RoleplayService(LlmChatProvider llmChatProvider) {
        this.llmChatProvider = llmChatProvider;
    }

    public List<RoleplayPersona> personas() {
        return List.of(RoleplayPersona.values());
    }

    public SseEmitter streamReply(RoleplayChatRequest request) {
        RoleplayPersona persona = RoleplayPersona.fromCode(request.getPersona())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "未知角色"));

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(persona.systemPrompt()));
        for (RoleplayMessage m : request.getMessages()) {
            String role = "assistant".equalsIgnoreCase(m.getRole()) ? "assistant" : "user";
            messages.add(new ChatMessage(role, m.getContent()));
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        CompletableFuture.runAsync(() -> {
            try {
                llmChatProvider.streamChat(LlmScene.GENERAL, messages,
                        delta -> sendQuietly(emitter, "delta", delta));
                sendQuietly(emitter, "done", "");
                emitter.complete();
            } catch (Exception e) {
                log.warn("Roleplay chat failed persona={}: {}", persona.code(), e.toString());
                sendQuietly(emitter, "error", "对方暂时无法回应，请稍后再试");
                emitter.complete();
            }
        });
        return emitter;
    }

    private void sendQuietly(SseEmitter emitter, String event, String data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException | IllegalStateException e) {
            // 客户端断开或已完成：忽略
        }
    }
}
