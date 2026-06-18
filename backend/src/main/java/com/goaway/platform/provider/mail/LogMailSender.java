package com.goaway.platform.provider.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.auth.password-reset.provider", havingValue = "log", matchIfMissing = true)
public class LogMailSender implements MailSender {
    private static final Logger logger = LoggerFactory.getLogger(LogMailSender.class);

    @Override
    public ProviderMode providerMode() {
        return ProviderMode.LOG;
    }

    @Override
    public void sendPasswordResetCode(String email, String code) {
        logger.info("Password reset code for {} is {} (log provider)", email, code);
    }

    @Override
    public void sendEmailChangeCode(String email, String code) {
        logger.info("Email change code for {} is {} (log provider)", email, code);
    }
}
