package com.goaway.contexts.taunt.application;

import com.goaway.contexts.roleplay.domain.PersonaPromptBuilder;
import com.goaway.contexts.roleplay.domain.RoleplayPersona;
import com.goaway.contexts.taunt.domain.TauntTrigger;
import com.goaway.contexts.workprofile.domain.WorkProfile;
import com.goaway.contexts.workprofile.infrastructure.persistence.WorkProfileRepository;
import com.goaway.platform.llm.LlmScene;
import com.goaway.platform.provider.llm.LlmChatProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 千人千面地生成一句毒舌推送：优先用用户「最讨厌的人」画像，无画像回退预设角色；
 * LLM 失败/空结果时回退到触发类型自带的兜底文案，保证一定有内容可发。
 */
@Service
public class TauntContentService {

    private static final Logger log = LoggerFactory.getLogger(TauntContentService.class);
    private static final int MAX_LEN = 60;

    private final WorkProfileRepository workProfileRepository;
    private final LlmChatProvider llmChatProvider;

    public TauntContentService(WorkProfileRepository workProfileRepository, LlmChatProvider llmChatProvider) {
        this.workProfileRepository = workProfileRepository;
        this.llmChatProvider = llmChatProvider;
    }

    public String generate(Long userId, TauntTrigger trigger) {
        String roleSetup = resolveRoleSetup(userId);
        String system = PersonaPromptBuilder.tauntSystemPrompt(roleSetup);
        try {
            // 复用 GENERAL 场景（毒鸡汤/骂老板同类短文本），避免演进 enum 触发 DB check 约束迁移
            String raw = llmChatProvider.chat(LlmScene.GENERAL, system, trigger.contextHint());
            String cleaned = sanitize(raw);
            if (!cleaned.isBlank()) {
                return cleaned;
            }
            log.debug("Taunt LLM 空结果 userId={} trigger={}，用兜底", userId, trigger);
        } catch (Exception e) {
            log.warn("Taunt LLM 生成失败 userId={} trigger={}: {}，用兜底", userId, trigger, e.toString());
        }
        return trigger.fallbackLine();
    }

    private String resolveRoleSetup(Long userId) {
        WorkProfile profile = workProfileRepository.findByUserId(userId).orElse(null);
        String description = describeHatedPerson(profile);
        if (description != null) {
            return PersonaPromptBuilder.customRoleSetup(description);
        }
        // 没填「最讨厌的人」：按 userId 稳定地挑一个预设角色，保证不同用户语气有差异
        RoleplayPersona[] presets = RoleplayPersona.values();
        RoleplayPersona persona = presets[(int) Math.floorMod(userId == null ? 0 : userId, presets.length)];
        return persona.roleSetup();
    }

    /** 把画像里的「最讨厌的人」拼成一句描述；全空返回 null。 */
    private String describeHatedPerson(WorkProfile profile) {
        if (profile == null) {
            return null;
        }
        String relation = blankToNull(profile.getHatedRelation());
        String nickname = blankToNull(profile.getHatedNickname());
        String traits = blankToNull(profile.getHatedTraits());
        if (relation == null && nickname == null && traits == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (relation != null) {
            sb.append("是用户的").append(relation);
        }
        if (nickname != null) {
            sb.append(sb.length() > 0 ? "，" : "").append("用户私下叫 TA「").append(nickname).append("」");
        }
        if (traits != null) {
            sb.append(sb.length() > 0 ? "，" : "").append("特征：").append(traits);
        }
        return sb.toString();
    }

    private String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().replaceAll("\\s+", " ");
        // 去掉模型可能加的成对引号
        s = s.replaceAll("^[\"'“”‘’「」]+", "").replaceAll("[\"'“”‘’「」]+$", "").trim();
        if (s.length() > MAX_LEN) {
            s = s.substring(0, MAX_LEN);
        }
        return s;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
