package com.goaway.platform.llm;

public enum LlmScene {
    // AI 周报生成（流式）
    WEEKLY,
    // 通用文本生成（毒鸡汤 / 骂老板 / 毒舌推送等，复用一套模型配置）
    GENERAL
}
