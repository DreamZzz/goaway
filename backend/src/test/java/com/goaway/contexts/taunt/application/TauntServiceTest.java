package com.goaway.contexts.taunt.application;

import com.goaway.contexts.push.application.PushService;
import com.goaway.contexts.push.domain.PushFrequency;
import com.goaway.contexts.push.domain.PushPreference;
import com.goaway.contexts.taunt.domain.TauntLog;
import com.goaway.contexts.taunt.domain.TauntTrigger;
import com.goaway.contexts.taunt.infrastructure.persistence.TauntLogRepository;
import com.goaway.platform.provider.push.PushProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TauntServiceTest {

    private final TauntContentService contentService = mock(TauntContentService.class);
    private final PushService pushService = mock(PushService.class);
    private final PushProvider pushProvider = mock(PushProvider.class);
    private final TauntLogRepository tauntLogRepository = mock(TauntLogRepository.class);
    private final TauntService service =
            new TauntService(contentService, pushService, pushProvider, tauntLogRepository);

    private static final LocalDateTime NOON = LocalDateTime.of(2026, 6, 23, 14, 0);

    private PushPreference pref() {
        PushPreference p = new PushPreference(1L);
        p.setEnabled(true);
        p.setFrequency(PushFrequency.NORMAL); // 间隔 240 分钟、每日 3
        return p;
    }

    @Test
    @DisplayName("关闭或频率 OFF 不发")
    void canSendNow_falseWhenOff() {
        PushPreference p = pref();
        p.setEnabled(false);
        assertFalse(service.canSendNow(p, NOON));

        p.setEnabled(true);
        p.setFrequency(PushFrequency.OFF);
        assertFalse(service.canSendNow(p, NOON));
    }

    @Test
    @DisplayName("免打扰时段不发")
    void canSendNow_falseInQuietHour() {
        PushPreference p = pref(); // 默认 22-8 静音
        assertFalse(service.canSendNow(p, LocalDateTime.of(2026, 6, 23, 23, 0)));
        assertTrue(service.canSendNow(p, NOON));
    }

    @Test
    @DisplayName("达到每日上限不发")
    void canSendNow_falseWhenDailyCapReached() {
        when(tauntLogRepository.countByUserIdAndSuccessTrueAndSentAtAfter(eq(1L), any())).thenReturn(3L);
        assertFalse(service.canSendNow(pref(), NOON));
    }

    @Test
    @DisplayName("距上次未到间隔则未到期；到了间隔则到期")
    void isDue_respectsInterval() {
        when(tauntLogRepository.countByUserIdAndSuccessTrueAndSentAtAfter(eq(1L), any())).thenReturn(0L);

        PushPreference recent = pref();
        recent.setLastTauntAt(NOON.minusMinutes(100)); // < 240
        assertFalse(service.isDue(recent, NOON));

        PushPreference old = pref();
        old.setLastTauntAt(NOON.minusMinutes(300)); // > 240
        assertTrue(service.isDue(old, NOON));
    }

    @Test
    @DisplayName("投递成功：记录日志且更新水位")
    void deliver_success_marksTauntedAndLogs() {
        when(pushService.deviceTokensForUser(1L)).thenReturn(List.of("tk1"));
        when(contentService.generate(1L, TauntTrigger.SCHEDULED)).thenReturn("又摸鱼？");
        when(pushProvider.sendBatch(any())).thenReturn(List.of(PushProvider.PushResult.ok()));

        TauntService.DeliverResult result = service.deliver(1L, TauntTrigger.SCHEDULED);

        assertTrue(result.success());
        assertEquals("又摸鱼？", result.content());
        verify(pushService).markTaunted(1L);
        ArgumentCaptor<TauntLog> logCaptor = ArgumentCaptor.forClass(TauntLog.class);
        verify(tauntLogRepository).save(logCaptor.capture());
        assertTrue(logCaptor.getValue().isSuccess());
    }

    @Test
    @DisplayName("token 失效：删除设备且不更新水位")
    void deliver_invalidToken_removesDevice() {
        when(pushService.deviceTokensForUser(1L)).thenReturn(List.of("dead"));
        when(contentService.generate(1L, TauntTrigger.SCHEDULED)).thenReturn("x");
        when(pushProvider.sendBatch(any())).thenReturn(List.of(PushProvider.PushResult.invalid("410")));

        TauntService.DeliverResult result = service.deliver(1L, TauntTrigger.SCHEDULED);

        assertFalse(result.success());
        verify(pushService).removeDevice("dead");
        verify(pushService, never()).markTaunted(anyLong());
    }

    @Test
    @DisplayName("无设备 token：仍生成内容但不发送")
    void deliver_noTokens() {
        when(pushService.deviceTokensForUser(1L)).thenReturn(List.of());
        when(contentService.generate(1L, TauntTrigger.SCHEDULED)).thenReturn("y");

        TauntService.DeliverResult result = service.deliver(1L, TauntTrigger.SCHEDULED);

        assertFalse(result.success());
        assertEquals("y", result.content());
        verify(pushProvider, never()).sendBatch(any());
    }
}
