package com.goaway.contexts.account.application;

/**
 * Thrown by {@link VerificationCodeService} when a code is requested for a subject
 * that already received one within the cooldown window.
 */
public class VerificationCodeCooldownException extends RuntimeException {

    private final long remainingSeconds;

    public VerificationCodeCooldownException(long remainingSeconds) {
        super("验证码已发送，请 " + remainingSeconds + " 秒后再试");
        this.remainingSeconds = remainingSeconds;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }
}
