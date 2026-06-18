package com.goaway.platform.provider.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.goaway.platform.llm.LlmScene;
import com.goaway.platform.llm.LlmSceneConfig;
import com.goaway.platform.llm.LlmSceneConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 调用 OpenAI 兼容的 /chat/completions（stream=true）做流式文本生成。
 * 模型 / base-url / api-key / 超时来自 LlmSceneConfig（DB 为事实源，env 为默认）。
 */
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "openai-compatible")
public class OpenAiCompatibleLlmChatProvider implements LlmChatProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmChatProvider.class);

    private final LlmSceneConfigService sceneConfigService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public OpenAiCompatibleLlmChatProvider(LlmSceneConfigService sceneConfigService) {
        this.sceneConfigService = sceneConfigService;
    }

    @Override
    public void streamChat(LlmScene scene, List<ChatMessage> messages, Consumer<String> onDelta) {
        LlmSceneConfig config = sceneConfigService.getConfig(scene);
        String url = resolveChatUrl(config.getBaseUrl());

        String requestBody = buildRequestBody(config.getModel(), messages);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(Math.max(15_000, config.getTimeoutMs())))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<Stream<String>> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() / 100 != 2) {
                throw new LlmGenerationException("LLM 返回状态码 " + response.statusCode());
            }
            try (Stream<String> lines = response.body()) {
                lines.forEach(line -> handleLine(line, onDelta));
            }
        } catch (LlmGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("LLM streaming failed scene={} model={}: {}", scene, config.getModel(), e.toString());
            throw new LlmGenerationException("LLM 生成失败：" + e.getMessage(), e);
        }
    }

    private void handleLine(String line, Consumer<String> onDelta) {
        if (line == null || line.isBlank() || !line.startsWith("data:")) {
            return;
        }
        String payload = line.substring("data:".length()).trim();
        if (payload.isEmpty() || "[DONE]".equals(payload)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode delta = root.path("choices").path(0).path("delta").path("content");
            if (delta.isTextual()) {
                String text = delta.asText();
                if (!text.isEmpty()) {
                    onDelta.accept(text);
                }
            }
        } catch (Exception e) {
            // 单行解析失败不应中断整体流
            log.debug("Skip unparseable SSE line: {}", payload);
        }
    }

    private String buildRequestBody(String model, List<ChatMessage> messages) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("stream", true);
        ArrayNode arr = root.putArray("messages");
        for (ChatMessage m : messages) {
            ObjectNode node = arr.addObject();
            node.put("role", m.role());
            node.put("content", m.content() == null ? "" : m.content());
        }
        return root.toString();
    }

    private String resolveChatUrl(String baseUrl) {
        String base = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        if (base.endsWith("/v1")) {
            return base + "/chat/completions";
        }
        return base + "/v1/chat/completions";
    }
}
