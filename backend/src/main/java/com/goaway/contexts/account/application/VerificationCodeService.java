package com.goaway.contexts.account.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class VerificationCodeService {
    // Single-instance in-memory storage keeps the auth flows simple for local and
    // current ECS deployments. Multi-instance production would need shared storage.
    private final Map<String, VerificationCodeEntry> codes = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastIssued = new ConcurrentHashMap<>();
    private final long codeTtlSeconds;
    private final long cooldownSeconds;

    public VerificationCodeService(
            @Value("${app.auth.verification-code-ttl-seconds:600}") long codeTtlSeconds,
            @Value("${app.auth.verification-code-cooldown-seconds:60}") long cooldownSeconds) {
        this.codeTtlSeconds = codeTtlSeconds;
        this.cooldownSeconds = cooldownSeconds;
    }

    public String issueCode(String subject, String purpose) {
        cleanupExpired();

        String normalizedKey = buildKey(subject, purpose);
        Instant last = lastIssued.get(normalizedKey);
        if (last != null) {
            long elapsed = Instant.now().getEpochSecond() - last.getEpochSecond();
            if (elapsed < cooldownSeconds) {
                throw new VerificationCodeCooldownException(cooldownSeconds - elapsed);
            }
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        codes.put(normalizedKey, new VerificationCodeEntry(code, Instant.now().plusSeconds(codeTtlSeconds)));
        lastIssued.put(normalizedKey, Instant.now());
        return code;
    }

    public boolean verifyCode(String subject, String purpose, String code) {
        cleanupExpired();

        VerificationCodeEntry entry = codes.get(buildKey(subject, purpose));
        if (entry == null || code == null || code.isBlank()) {
            return false;
        }

        boolean matched = entry.code().equals(code.trim());
        if (matched) {
            codes.remove(buildKey(subject, purpose));
        }
        return matched;
    }

    public void revokeCode(String subject, String purpose) {
        codes.remove(buildKey(subject, purpose));
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        codes.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private String buildKey(String subject, String purpose) {
        return (purpose + ":" + subject).toLowerCase(Locale.ROOT);
    }

    private record VerificationCodeEntry(String code, Instant expiresAt) {}
}
