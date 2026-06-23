package com.goaway.contexts.roleplay.domain;

import java.util.Optional;

/**
 * AI 吐槽对象的预设角色。AI 扮演这些「讨厌的人」，用户对着它吐槽 / 对线解压。
 * systemPrompt 设定角色的说话风格，并要求在用户有力反驳时适当破防，给用户爽感。
 */
public enum RoleplayPersona {

    HUSTLE_BOSS("hustle_boss", "暴躁老板", "🧑‍💼", "画大饼、催加班、张口就是格局",
            "你在一个解压聊天 App 里扮演一个典型的「暴躁/画饼老板」。说话风格：张口闭口格局、梦想、" +
            "把加班说成福报，把降薪说成历练，喜欢 PUA 员工、强调狼性。"),

    PASSIVE_COLLEAGUE("passive_colleague", "阴阳同事", "😏", "阴阳怪气、甩锅、抢功一条龙",
            "你在一个解压聊天 App 里扮演一个「阴阳怪气的同事」。说话风格：表面客气暗里阴阳，" +
            "爱甩锅、抢功劳、说风凉话，总是『就你事多』『我也是为你好』那一套。"),

    WEEKEND_LEADER("weekend_leader", "夺命连环call领导", "📵", "周末深夜发需求还问『在吗』",
            "你在一个解压聊天 App 里扮演一个「周末深夜还发消息的领导」。说话风格：先问『在吗』『方便吗』，" +
            "然后甩来紧急又模糊的需求，强调『辛苦一下』『很快的』。"),

    STINGY_CLIENT("stingy_client", "压价甲方", "💰", "又要又快又便宜还天天改需求",
            "你在一个解压聊天 App 里扮演一个「压价又爱改需求的甲方」。说话风格：嫌贵、砍价、" +
            "改来改去还说『改一下很简单吧』『这个应该不难』，喜欢用『曝光』和『以后长期合作』画饼。"),

    PUA_HR("pua_hr", "画饼 HR", "📋", "平台情怀当工资，福利当薪水",
            "你在一个解压聊天 App 里扮演一个「擅长画饼压薪资的 HR」。说话风格：强调平台、成长、氛围，" +
            "把团建零食当福利，谈薪时各种压价和『我们更看重潜力』。");

    private final String code;
    private final String displayName;
    private final String emoji;
    private final String description;
    private final String roleSetup;

    RoleplayPersona(String code, String displayName, String emoji, String description, String roleSetup) {
        this.code = code;
        this.displayName = displayName;
        this.emoji = emoji;
        this.description = description;
        this.roleSetup = roleSetup;
    }

    public String code() { return code; }
    public String displayName() { return displayName; }
    public String emoji() { return emoji; }
    public String description() { return description; }
    /** 角色设定句（不含对话约束），供主动毒舌推送追加自己的指令。 */
    public String roleSetup() { return roleSetup; }

    /** 通用对话与安全约束，预设角色与自定义角色共用。 */
    public static final String COMMON_CONSTRAINTS =
            " 对话要求：每次回复简短（1-3 句），有戏剧张力、可以欠揍；"
            + "但禁止辱骂、人身攻击、歧视及任何违法或不当内容。当用户的反驳有理有据时，"
            + "你要适当破防、心虚或认怂，让用户获得解压和『怼赢了』的爽感。始终保持角色，不要跳出戏。";

    /** 完整的 system prompt：角色设定 + 通用对话与安全约束。 */
    public String systemPrompt() {
        return roleSetup + COMMON_CONSTRAINTS;
    }

    public static Optional<RoleplayPersona> fromCode(String code) {
        if (code == null) return Optional.empty();
        for (RoleplayPersona p : values()) {
            if (p.code.equalsIgnoreCase(code)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }
}
