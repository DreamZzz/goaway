package com.goaway.contexts.weekly.application;

import com.goaway.contexts.weekly.domain.WeeklyReport;
import com.goaway.contexts.weekly.infrastructure.persistence.WeeklyReportRepository;
import com.goaway.platform.llm.LlmScene;
import com.goaway.platform.provider.llm.LlmChatProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class WeeklyService {

    private static final Logger log = LoggerFactory.getLogger(WeeklyService.class);
    private static final long SSE_TIMEOUT_MS = 120_000;

    private final WeeklyReportRepository repository;
    private final LlmChatProvider llmChatProvider;

    public WeeklyService(WeeklyReportRepository repository, LlmChatProvider llmChatProvider) {
        this.repository = repository;
        this.llmChatProvider = llmChatProvider;
    }

    public static String currentWeekKey(LocalDate date) {
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        return String.format("%d-W%02d", year, week);
    }

    /**
     * 流式生成周报：先建记录(GENERATING)，异步调用 LLM 并把增量推给前端，
     * 完成后落库为 DONE 并发送 done 事件。
     */
    public SseEmitter streamGenerate(Long userId, String fragments) {
        WeeklyReport report = repository.save(
                new WeeklyReport(userId, currentWeekKey(LocalDate.now()), fragments));
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        CompletableFuture.runAsync(() -> {
            StringBuilder full = new StringBuilder();
            try {
                llmChatProvider.streamChat(
                        LlmScene.WEEKLY,
                        WeeklyPrompts.SYSTEM,
                        WeeklyPrompts.buildUserPrompt(fragments),
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
