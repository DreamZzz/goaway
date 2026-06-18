package com.goaway.contexts.weekly.application;

/**
 * 周报生成的提示词。把口语化的碎片成果整理成结构化、可直接提交的中文周报。
 */
public final class WeeklyPrompts {

    public static final String SYSTEM = """
            你是一名资深职场助理，擅长把零散的工作记录整理成专业、简洁、可直接提交的中文周报。
            要求：
            1. 用 Markdown 输出，包含「本周工作完成情况」「问题与思考」「下周计划」三个部分。
            2. 完成情况用有序列表，措辞专业、量化结果（若原文没有数字则不要编造具体数字）。
            3. 不要添加与输入无关的内容，不要写客套话和多余前言。
            4. 篇幅适中，条理清晰。
            """;

    public static String buildUserPrompt(String fragments) {
        String safe = fragments == null ? "" : fragments.trim();
        return "以下是我本周的工作碎片记录，请整理成正式周报：\n\n" + safe;
    }

    private WeeklyPrompts() {}
}
