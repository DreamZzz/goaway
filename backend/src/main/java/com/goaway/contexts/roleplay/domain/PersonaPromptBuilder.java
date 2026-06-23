package com.goaway.contexts.roleplay.domain;

/**
 * 角色 system prompt 的共享拼装：交互对线（RoleplayService）与主动毒舌推送（taunt 域）共用，
 * 保证「最讨厌的人」语气一致。纯领域工具，无 web / 持久化依赖。
 */
public final class PersonaPromptBuilder {

    private PersonaPromptBuilder() {}

    /**
     * 自定义「最讨厌的人」的角色设定句（不含对话约束），由调用方按场景追加约束。
     */
    public static String customRoleSetup(String description) {
        return "你在一个解压聊天 App 里扮演「用户最讨厌的一个人」。这个人的身份与特征："
                + description.trim()
                + "。请逼真还原 TA 的说话风格、口吻与让人来气的点。";
    }

    /**
     * 主动推送场景的附加指令：让模型只产出一句「挑衅式推送文案」，勾用户点开进来对线。
     */
    public static final String TAUNT_INSTRUCTION =
            " 现在你要【主动】给这个人发一条手机推送通知：用你那让人来气的语气挑衅/嘲讽 TA，"
            + "勾得 TA 忍不住点开 App 进来跟你对线。要求：只输出推送正文一句话，15~30 字，口语、扎心、有钩子；"
            + "不要加引号、不要解释、不要堆 emoji。禁止辱骂、人身攻击、歧视及任何违法不当内容。";

    /**
     * 毒舌推送 system prompt = 角色设定 + 推送附加指令。
     */
    public static String tauntSystemPrompt(String roleSetup) {
        return roleSetup + TAUNT_INSTRUCTION;
    }
}
