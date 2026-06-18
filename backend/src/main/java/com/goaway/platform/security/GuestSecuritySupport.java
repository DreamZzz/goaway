package com.goaway.platform.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Component
public class GuestSecuritySupport {

    public static final String GUEST_INSTALLATION_HEADER = "X-Guest-Installation-Id";
    public static final String ERROR_ACCOUNT_REQUIRED = "ACCOUNT_REQUIRED";
    public static final String ERROR_INVALID_GUEST_CONTEXT = "INVALID_GUEST_CONTEXT";
    public static final String ERROR_GUEST_TRIAL_EXHAUSTED = "GUEST_TRIAL_EXHAUSTED";

    @Value("${app.guest.install-id-salt:${app.jwt.secret}}")
    private String installIdSalt;

    @Value("${app.guest.ip-hash-salt:${app.jwt.secret}}")
    private String ipHashSalt;

    @Value("${app.guest.trial-limit:3}")
    private int trialLimit;

    public boolean isValidInstallationId(String installationId) {
        if (installationId == null || installationId.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(installationId);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public String hashInstallationId(String installationId) {
        return sha256(installationId + ":" + installIdSalt);
    }

    public String hashClientIp(String clientIp) {
        return sha256((clientIp == null ? "" : clientIp) + ":" + ipHashSalt);
    }

    public String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public int getTrialLimit() {
        return trialLimit;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash guest context", exception);
        }
    }
}
