package com.goaway.platform.llm;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "llm_scene_config")
public class LlmSceneConfig {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private LlmScene scene;

    @Column(name = "base_url", nullable = false, length = 255)
    private String baseUrl;

    @Column(name = "api_key", nullable = false, length = 255)
    private String apiKey;

    @Column(name = "model", nullable = false, length = 128)
    private String model;

    @Column(name = "fallback_model", length = 128)
    private String fallbackModel;

    @Column(name = "timeout_ms", nullable = false)
    private int timeoutMs;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public LlmScene getScene() { return scene; }
    public void setScene(LlmScene scene) { this.scene = scene; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getFallbackModel() { return fallbackModel; }
    public void setFallbackModel(String fallbackModel) { this.fallbackModel = fallbackModel; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
