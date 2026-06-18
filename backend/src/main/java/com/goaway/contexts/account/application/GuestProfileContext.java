package com.goaway.contexts.account.application;

public record GuestProfileContext(
        Long userId,
        Long guestProfileId,
        String installIdHash,
        String ipHash,
        int trialUsedCount,
        int trialLimit
) {
    public int trialRemaining() {
        return Math.max(0, trialLimit - trialUsedCount);
    }
}
