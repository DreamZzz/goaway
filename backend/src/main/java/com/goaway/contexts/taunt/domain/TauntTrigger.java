package com.goaway.contexts.taunt.domain;

/**
 * 毒舌推送的触发类型，决定给 LLM 的场景提示与兜底文案池。
 */
public enum TauntTrigger {

    SCHEDULED("随便挑个由头，损我一句、勾我进来跟你对线。",
            "又在摸鱼了吧？有种进来跟我说清楚。"),
    RECALL("用户已经好几天没打开 App 了。用你的语气阴阳他、激他回来跟你对线。",
            "几天不见就怂了？不敢来跟我对线了？"),
    SCENE_MONDAY("现在是周一早高峰，打工人刚被闹钟拖起来。用你的语气在伤口上撒把盐。",
            "周一了，梦想还没实现，先去搬砖吧。"),
    SCENE_FRIDAY("现在是周五傍晚快下班，打工人正盼着周末。用你的语气恶心他一下。",
            "周末？需求我已经给你排好了，别急着走。");

    private final String contextHint;
    private final String fallbackLine;

    TauntTrigger(String contextHint, String fallbackLine) {
        this.contextHint = contextHint;
        this.fallbackLine = fallbackLine;
    }

    public String contextHint() { return contextHint; }
    public String fallbackLine() { return fallbackLine; }
}
