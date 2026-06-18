package com.goaway.platform.provider.sms;

import com.goaway.contexts.account.application.SmsDeliveryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmsDeliveryServiceTest {

    @Test
    @DisplayName("facade should delegate to configured sms sender")
    void smsDeliveryFacade_ShouldDelegateToConfiguredProvider() {
        SmsSender smsSender = mock(SmsSender.class);
        SmsDeliveryService smsDeliveryService = new SmsDeliveryService(smsSender);

        assertDoesNotThrow(() -> smsDeliveryService.sendLoginCode("13800000000", "123456"));
        verify(smsSender).sendLoginCode("13800000000", "123456");
    }

    @Test
    @DisplayName("log provider should not require Aliyun configuration")
    void logSmsSender_ShouldAllowLogProviderWithoutAliyunConfig() {
        LogSmsSender smsSender = new LogSmsSender();

        assertDoesNotThrow(() -> smsSender.sendLoginCode("13800000000", "123456"));
    }
}
