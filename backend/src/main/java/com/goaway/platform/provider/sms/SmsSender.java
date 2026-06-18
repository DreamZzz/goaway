package com.goaway.platform.provider.sms;

public interface SmsSender {
    enum ProviderMode {
        LOG
    }

    ProviderMode providerMode();

    void sendLoginCode(String phone, String code);
}
