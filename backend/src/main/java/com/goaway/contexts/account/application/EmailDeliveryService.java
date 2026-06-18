package com.goaway.contexts.account.application;

import com.goaway.platform.provider.mail.MailSender;
import com.goaway.platform.provider.mail.EmailDeliveryException;
import org.springframework.stereotype.Service;

@Service
public class EmailDeliveryService {
    private final MailSender mailSender;

    public EmailDeliveryService(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public MailSender.ProviderMode getProviderMode() {
        return mailSender.providerMode();
    }

    public void sendPasswordResetCode(String email, String code) {
        mailSender.sendPasswordResetCode(email, code);
    }

    public void sendEmailChangeCode(String email, String code) {
        mailSender.sendEmailChangeCode(email, code);
    }
}
