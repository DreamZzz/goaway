package com.goaway.contexts.weekly.application;

import com.goaway.contexts.activity.domain.ActivityType;
import com.goaway.contexts.activity.domain.BadgeSeries;
import com.goaway.contexts.activity.domain.BadgeTier;
import com.goaway.contexts.activity.domain.UserBadge;
import com.goaway.contexts.activity.infrastructure.persistence.ActivityEventRepository;
import com.goaway.contexts.activity.infrastructure.persistence.BadgeDefinitionRepository;
import com.goaway.contexts.activity.infrastructure.persistence.UserBadgeRepository;
import com.goaway.contexts.checkin.infrastructure.persistence.CheckinRecordRepository;
import com.goaway.contexts.leaderboard.api.dto.LeaderboardDTO;
import com.goaway.contexts.leaderboard.application.LeaderboardQuery.Board;
import com.goaway.contexts.leaderboard.application.LeaderboardService;
import com.goaway.contexts.weekly.domain.WeeklyReport;
import com.goaway.contexts.weekly.infrastructure.persistence.WeeklyReportRepository;
import com.goaway.platform.llm.LlmScene;
import com.goaway.platform.provider.llm.LlmChatProvider;
import com.goaway.shared.time.PeriodRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class WeeklyService {

    private static final Logger log = LoggerFactory.getLogger(WeeklyService.class);
    private static final long SSE_TIMEOUT_MS = 120_000;
    private static final String REFLECTION_HEADING = "\n\n## 🍵 本周思考（毒鸡汤）\n\n";

    private final WeeklyReportRepository repository;
    private final LlmChatProvider llmChatProvider;
    private final ActivityEventRepository activityRepo;
    private final UserBadgeRepository userBadgeRepo;
    private final CheckinRecordRepository checkinRepo;
    private final LeaderboardService leaderboardService;
    private final BadgeDefinitionRepository badgeDefRepo;

    public WeeklyService(WeeklyReportRepository repository, LlmChatProvider llmChatProvider,
                         ActivityEventRepository activityRepo, UserBadgeRepository userBadgeRepo,
                         CheckinRecordRepository checkinRepo, LeaderboardService leaderboardService,
                         BadgeDefinitionRepository badgeDefRepo) {
        this.repository = repository;
        this.llmChatProvider = llmChatProvider;
        this.activityRepo = activityRepo;
        this.userBadgeRepo = userBadgeRepo;
        this.checkinRepo = checkinRepo;
        this.leaderboardService = leaderboardService;
        this.badgeDefRepo = badgeDefRepo;
    }

    public static String currentWeekKey(LocalDate date) {
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        return String.format("%d-W%02d", year, week);
    }

    /**
     * 流式生成本周周报：基于本周真实使用数据（次数/上榜/新勋章）拼统计段，
     * 再由 LLM 生成一段阴阳怪气毒鸡汤作为「本周思考」。
     */
    public SseEmitter streamGenerate(Long userId) {
        LocalDate today = LocalDate.now();
        PeriodRange range = PeriodRange.of(PeriodRange.Period.WEEK, today);
        LocalDateTime start = range.start().atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        String statsMarkdown = buildStatsMarkdown(userId, today, range, start, end);
        String digestForLlm = stripMarkdown(statsMarkdown);

        WeeklyReport report = repository.save(new WeeklyReport(userId, currentWeekKey(today), digestForLlm));
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        CompletableFuture.runAsync(() -> {
            StringBuilder full = new StringBuilder();
            full.append(statsMarkdown).append(REFLECTION_HEADING);
            // 先把统计段（含思考标题）推给前端
            sendQuietly(emitter, "delta", statsMarkdown + REFLECTION_HEADING);
            try {
                llmChatProvider.streamChat(
                        LlmScene.WEEKLY,
                        WeeklyPrompts.SYSTEM,
                        WeeklyPrompts.buildUserPrompt(digestForLlm),
                        delta -> {
                            full.append(delta);
                            sendQuietly(emitter, "delta", delta);
                        });
                report.setContent(full.toString());
                report.setStatus(WeeklyReport.Status.DONE);
                repository.save(report);
                sendQuietly(emitter, "done", String.valueOf(report.getId()));
                emitter.complete();
            } catch (Exception e) {
                log.warn("Weekly report generation failed reportId={}: {}", report.getId(), e.toString());
                report.setStatus(WeeklyReport.Status.FAILED);
                report.setContent(full.toString());
                repository.save(report);
                sendQuietly(emitter, "error", "生成失败，请稍后重试");
                emitter.complete();
            }
        });

        return emitter;
    }

    private String buildStatsMarkdown(Long userId, LocalDate today, PeriodRange range,
                                      LocalDateTime start, LocalDateTime end) {
        long water = activityRepo.countByUserIdAndTypeAndOccurredAtBetween(userId, ActivityType.WATER, start, end);
        long smoke = activityRepo.countByUserIdAndTypeAndOccurredAtBetween(userId, ActivityType.SMOKE, start, end);
        long poop = activityRepo.countByUserIdAndTypeAndOccurredAtBetween(userId, ActivityType.POOP, start, end);
        long poopSec = activityRepo.sumDurationByUserIdAndTypeAndOccurredAtBetween(userId, ActivityType.POOP, start, end);
        long fishSec = activityRepo.sumDurationByUserIdAndTypeAndOccurredAtBetween(userId, ActivityType.FISH, start, end);
        long checkinDays = checkinRepo.countByUserIdAndCheckinDateBetween(userId, range.start(), range.end());

        StringBuilder sb = new StringBuilder();
        sb.append("## 📊 本周打工流水（").append(currentWeekKey(today)).append("）\n");
        sb.append("- 🐟 摸鱼 ").append(fmtDuration(fishSec)).append("\n");
        sb.append("- 🚽 带薪如厕 ").append(poop).append(" 次（共 ").append(fmtDuration(poopSec)).append("）\n");
        sb.append("- 💧 喝水 ").append(water).append(" 杯\n");
        sb.append("- 🚬 抽烟 ").append(smoke).append(" 根\n");
        sb.append("- ✅ 打卡 ").append(checkinDays).append(" 天\n");

        // 本周上榜（遍历内置榜，取有名次的）
        List<String> placements = new ArrayList<>();
        for (Board b : Board.values()) {
            LeaderboardDTO dto = leaderboardService.load(b.code(), "all", null, "week", userId);
            if (dto.getMyRank() != null) {
                placements.add("「" + b.label + "」第 " + dto.getMyRank() + " 名");
            }
        }
        sb.append("\n## 🏆 本周上榜\n");
        if (placements.isEmpty()) {
            sb.append("- 本周没挤进任何榜单，泯然众人矣。\n");
        } else {
            for (String p : placements) sb.append("- ").append(p).append("\n");
        }

        // 本周新解锁勋章（无法解析的遗留键跳过）
        List<String> badges = new ArrayList<>();
        for (UserBadge ub : userBadgeRepo.findByUserIdAndEarnedAtBetween(userId, start, end)) {
            String label = labelOfBadge(ub.getBadgeKey());
            if (label != null) badges.add(label);
        }
        sb.append("\n## 🎖 本周新解锁\n");
        if (badges.isEmpty()) {
            sb.append("- 本周颗粒无收，勋章墙在吃灰。\n");
        } else {
            for (String b : badges) sb.append("- ").append(b).append("\n");
        }
        return sb.toString();
    }

    /** 勋章键 → 展示名："系列 · 档位" 或 配置勋章标题；无法解析（遗留键）返回 null。 */
    private String labelOfBadge(String key) {
        int dot = key.indexOf('.');
        if (dot < 0) {
            return badgeDefRepo.findByKey(key).map(d -> d.getTitle()).orElse(null);
        }
        Optional<BadgeSeries> series = BadgeSeries.byKey(key.substring(0, dot));
        if (series.isEmpty()) return null;
        try {
            BadgeTier tier = BadgeTier.valueOf(key.substring(dot + 1));
            return series.get().getTitle() + " · " + tier.getLabel();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String fmtDuration(long sec) {
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        if (h > 0) return m > 0 ? h + " 小时 " + m + " 分" : h + " 小时";
        if (m > 0) return m + " 分";
        return sec + " 秒";
    }

    /** 把统计 Markdown 压成纯文本喂给 LLM 当上下文。 */
    private String stripMarkdown(String md) {
        return md.replaceAll("[#*`]", "").trim();
    }

    private void sendQuietly(SseEmitter emitter, String event, String data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException | IllegalStateException e) {
            // 客户端断开或已完成：忽略
        }
    }

    public List<WeeklyReport> listRecent(Long userId) {
        return repository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<WeeklyReport> get(Long userId, Long id) {
        return repository.findByIdAndUserId(id, userId);
    }
}
