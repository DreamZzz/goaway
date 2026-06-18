package com.goaway.platform.provider.llm;

import com.goaway.platform.llm.LlmScene;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * 本地默认 LLM provider：不调用外部模型，按场景拼出占位输出并分段流式回放，
 * 便于本地开发与测试。WEEKLY 生成结构化周报；其余（如 roleplay）生成一段角色化回应。
 */
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmChatProvider implements LlmChatProvider {

    @Override
    public void streamChat(LlmScene scene, List<ChatMessage> messages, Consumer<String> onDelta) {
        String systemPrompt = firstSystem(messages);
        String lastUser = lastUser(messages);
        String body = scene == LlmScene.WEEKLY
                ? buildMockReport(lastUser)
                : buildMockReply(systemPrompt, lastUser);
        for (String segment : body.split("(?<=[\n。！？!?])")) {
            if (!segment.isEmpty()) {
                onDelta.accept(segment);
            }
        }
    }

    private String firstSystem(List<ChatMessage> messages) {
        return messages.stream()
                .filter(m -> "system".equals(m.role()))
                .map(ChatMessage::content)
                .findFirst().orElse("");
    }

    private String lastUser(List<ChatMessage> messages) {
        String content = "";
        for (ChatMessage m : messages) {
            if ("user".equals(m.role())) {
                content = m.content();
            }
        }
        return content == null ? "" : content.trim();
    }

    // ---- roleplay 占位回应 ----
    private String buildMockReply(String systemPrompt, String userText) {
        String role = guessRole(systemPrompt);
        String snippet = userText.isEmpty() ? "这事" : userText.replaceAll("\\s+", "").substring(0, Math.min(12, userText.replaceAll("\\s+", "").length()));
        return String.format(
                "（%s）「%s」？你这话说得就有意思了。我跟你讲，年轻人要多从自己身上找原因。"
                        + "不过……行吧，你说的好像也有那么点道理，这次先这样。\n"
                        + "（本回复由本地 mock 生成，仅供开发联调）",
                role, snippet);
    }

    private String guessRole(String systemPrompt) {
        if (systemPrompt == null) return "对方";
        if (systemPrompt.contains("老板")) return "老板";
        if (systemPrompt.contains("同事")) return "同事";
        if (systemPrompt.contains("HR")) return "HR";
        if (systemPrompt.contains("甲方")) return "甲方";
        if (systemPrompt.contains("领导")) return "领导";
        return "对方";
    }

    // ---- weekly 周报 ----
    private String buildMockReport(String userPrompt) {
        String fragments = userPrompt == null ? "" : userPrompt.trim();
        int sep = fragments.indexOf("\n\n");
        if (sep >= 0) {
            fragments = fragments.substring(sep + 2).trim();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# 本周工作周报\n\n");
        sb.append("## 一、本周工作完成情况\n");
        if (fragments.isEmpty()) {
            sb.append("1. 推进了本周既定任务，整体进展顺利。\n");
            sb.append("2. 与相关同事协作，解决了若干阻塞问题。\n");
        } else {
            int idx = 1;
            for (String line : fragments.split("[\n;；]+")) {
                String item = line.trim();
                if (!item.isEmpty()) {
                    sb.append(idx++).append(". ").append(polish(item)).append("\n");
                }
            }
        }
        sb.append("\n## 二、问题与思考\n");
        sb.append("- 在推进过程中关注效率与质量的平衡，及时复盘改进。\n");
        sb.append("\n## 三、下周计划\n");
        sb.append("- 延续本周进展，聚焦重点目标，保持节奏。\n");
        sb.append("\n（本内容由本地 mock 生成，仅供开发联调）\n");
        return sb.toString();
    }

    private String polish(String raw) {
        String s = raw.replaceAll("^[-*\\d.、\\s]+", "");
        if (s.length() > 0 && !s.endsWith("。") && !s.endsWith("，")) {
            s = s + "，按计划完成";
        }
        return s;
    }
}
