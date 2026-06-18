package com.goaway.platform.llm;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LlmSceneConfigService {

    private static final Logger log = LoggerFactory.getLogger(LlmSceneConfigService.class);

    private final LlmSceneConfigRepository repository;
    private final ConcurrentHashMap<LlmScene, LlmSceneConfig> cache = new ConcurrentHashMap<>();

    private final String defaultBaseUrl;
    private final String defaultApiKey;
    private final String defaultChatModel;
    private final String defaultFallbackModel;
    private final int defaultTimeoutMs;
    private final int defaultFallbackTimeoutMs;
    private final int defaultStreamingTimeoutMs;

    public LlmSceneConfigService(
            LlmSceneConfigRepository repository,
            @Value("${app.llm.openai.base-url:https://api.openai.com}") String defaultBaseUrl,
            @Value("${app.llm.openai.api-key:}") String defaultApiKey,
            @Value("${app.llm.openai.chat-model:gpt-4o-mini}") String defaultChatModel,
            @Value("${app.llm.openai.fallback-chat-model:}") String defaultFallbackModel,
            @Value("${app.llm.openai.timeout-ms:60000}") int defaultTimeoutMs,
            @Value("${app.llm.openai.fallback-timeout-ms:75000}") int defaultFallbackTimeoutMs,
            @Value("${app.llm.openai.streaming-timeout-ms:90000}") int defaultStreamingTimeoutMs) {
        this.repository = repository;
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultApiKey = defaultApiKey;
        this.defaultChatModel = defaultChatModel;
        this.defaultFallbackModel = defaultFallbackModel;
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.defaultFallbackTimeoutMs = defaultFallbackTimeoutMs;
        this.defaultStreamingTimeoutMs = defaultStreamingTimeoutMs;
    }

    @PostConstruct
    @Transactional
    public void initialize() {
        List<LlmSceneConfig> existing = repository.findAll();
        for (LlmSceneConfig config : existing) {
            cache.put(config.getScene(), config);
        }

        // Seed missing scenes from env-var defaults
        for (LlmScene scene : LlmScene.values()) {
            if (!cache.containsKey(scene)) {
                LlmSceneConfig seeded = seedDefault(scene);
                repository.save(seeded);
                cache.put(scene, seeded);
                log.info("Seeded LLM scene config for scene={} from env defaults", scene);
            }
        }

        for (LlmScene scene : LlmScene.values()) {
            warnIfEnvDefaultsDiffer(scene, cache.get(scene));
        }
    }

    private LlmSceneConfig seedDefault(LlmScene scene) {
        LlmSceneConfig config = new LlmSceneConfig();
        config.setScene(scene);
        config.setBaseUrl(defaultBaseUrl);
        config.setApiKey(defaultApiKey);
        config.setUpdatedAt(LocalDateTime.now());

        switch (scene) {
            case WEEKLY -> {
                // 流式生成，给更长的超时
                config.setModel(defaultChatModel);
                config.setFallbackModel(defaultFallbackModel != null && !defaultFallbackModel.isBlank()
                        ? defaultFallbackModel : null);
                config.setTimeoutMs(Math.max(Math.max(defaultTimeoutMs, defaultFallbackTimeoutMs),
                        defaultStreamingTimeoutMs));
            }
            case GENERAL -> {
                config.setModel(defaultChatModel);
                config.setFallbackModel(null);
                config.setTimeoutMs(defaultTimeoutMs);
            }
        }
        return config;
    }

    private void warnIfEnvDefaultsDiffer(LlmScene scene, LlmSceneConfig actual) {
        if (actual == null) {
            return;
        }

        LlmSceneConfig expected = seedDefault(scene);
        boolean differs = !equalsNullable(actual.getBaseUrl(), expected.getBaseUrl())
                || !equalsNullable(actual.getModel(), expected.getModel())
                || !equalsNullable(blankToNull(actual.getFallbackModel()), blankToNull(expected.getFallbackModel()))
                || actual.getTimeoutMs() != expected.getTimeoutMs();

        if (differs) {
            log.warn(
                    "LLM scene config mismatch for scene={}: env defaults baseUrl={} model={} fallbackModel={} timeoutMs={}, runtime DB config baseUrl={} model={} fallbackModel={} timeoutMs={}. Database config remains authoritative.",
                    scene,
                    expected.getBaseUrl(),
                    expected.getModel(),
                    blankToNull(expected.getFallbackModel()),
                    expected.getTimeoutMs(),
                    actual.getBaseUrl(),
                    actual.getModel(),
                    blankToNull(actual.getFallbackModel()),
                    actual.getTimeoutMs()
            );
        }
    }

    private static boolean equalsNullable(String left, String right) {
        return java.util.Objects.equals(blankToNull(left), blankToNull(right));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public LlmSceneConfig getConfig(LlmScene scene) {
        LlmSceneConfig config = cache.get(scene);
        if (config == null) {
            throw new IllegalStateException("LLM scene config not initialized for scene: " + scene);
        }
        return config;
    }

    @Transactional
    public LlmSceneConfig updateConfig(LlmScene scene, String baseUrl, String apiKey,
                                        String model, String fallbackModel, int timeoutMs) {
        LlmSceneConfig config = repository.findById(scene)
                .orElseGet(() -> {
                    LlmSceneConfig c = new LlmSceneConfig();
                    c.setScene(scene);
                    return c;
                });

        config.setBaseUrl(baseUrl);
        // null apiKey means keep existing
        if (apiKey != null && !apiKey.isBlank()) {
            config.setApiKey(apiKey);
        }
        config.setModel(model);
        config.setFallbackModel(fallbackModel);
        config.setTimeoutMs(timeoutMs);
        config.setUpdatedAt(LocalDateTime.now());

        LlmSceneConfig saved = repository.save(config);
        cache.put(scene, saved);
        log.info("Updated LLM scene config for scene={} baseUrl={} model={}", scene, baseUrl, model);
        return saved;
    }

    public List<LlmSceneConfig> listMasked() {
        return repository.findAll().stream()
                .map(config -> {
                    LlmSceneConfig masked = new LlmSceneConfig();
                    masked.setScene(config.getScene());
                    masked.setBaseUrl(config.getBaseUrl());
                    masked.setApiKey(maskApiKey(config.getApiKey()));
                    masked.setModel(config.getModel());
                    masked.setFallbackModel(config.getFallbackModel());
                    masked.setTimeoutMs(config.getTimeoutMs());
                    masked.setUpdatedAt(config.getUpdatedAt());
                    return masked;
                })
                .toList();
    }

    public static String maskApiKey(String key) {
        if (key == null || key.isBlank()) {
            return "••••••••";
        }
        if (key.length() <= 8) {
            return "*".repeat(key.length());
        }
        return "••••" + key.substring(key.length() - 8);
    }
}
