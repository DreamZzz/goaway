package com.goaway.contexts.account.application;

import com.goaway.contexts.account.domain.GuestProfile;
import com.goaway.contexts.account.domain.User;
import com.goaway.contexts.account.infrastructure.persistence.GuestProfileRepository;
import com.goaway.contexts.account.infrastructure.persistence.UserRepository;
import com.goaway.platform.security.GuestAccessException;
import com.goaway.platform.security.GuestSecuritySupport;
import com.goaway.platform.security.JwtUtils;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class GuestSessionService {

    private static final Logger log = LoggerFactory.getLogger(GuestSessionService.class);

    private final GuestProfileRepository guestProfileRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GuestSecuritySupport guestSecuritySupport;
    private final JwtUtils jwtUtils;

    public GuestSessionService(
            GuestProfileRepository guestProfileRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            GuestSecuritySupport guestSecuritySupport,
            JwtUtils jwtUtils
    ) {
        this.guestProfileRepository = guestProfileRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.guestSecuritySupport = guestSecuritySupport;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public GuestAuthSession issueGuestSession(String installationId, String clientIp, String requestId) {
        String installIdHash = guestSecuritySupport.hashInstallationId(installationId);
        String ipHash = guestSecuritySupport.hashClientIp(clientIp);
        Optional<GuestProfile> existingProfile = guestProfileRepository.findByInstallationHashForUpdate(installIdHash);
        GuestProfile profile = existingProfile
                .map(existing -> touchGuestProfile(existing, ipHash))
                .orElseGet(() -> createGuestProfile(installIdHash, ipHash));

        User user = profile.getUser();
        String token = jwtUtils.generateGuestToken(user.getUsername(), profile.getId(), installIdHash);
        int remaining = Math.max(0, guestSecuritySupport.getTrialLimit() - safeTrialCount(profile));

        log.info(
                "decision={} requestId={} guestProfileId={} installIdHash={} ipHash={} trialUsedCount={} reason={}",
                existingProfile.isPresent() ? "GUEST_REUSED" : "GUEST_ISSUED",
                requestId,
                profile.getId(),
                installIdHash,
                ipHash,
                safeTrialCount(profile),
                "guest_session"
        );

        return new GuestAuthSession(user, profile.getId(), token, remaining);
    }

    @Transactional(readOnly = true)
    public Optional<GuestProfileContext> resolveGuestProfile(Long userId, String installationId, String clientIp) {
        if (userId == null || installationId == null || installationId.isBlank()) {
            return Optional.empty();
        }
        return guestProfileRepository.findByUser_Id(userId)
                .map(profile -> new GuestProfileContext(
                        profile.getUser().getId(),
                        profile.getId(),
                        guestSecuritySupport.hashInstallationId(installationId),
                        guestSecuritySupport.hashClientIp(clientIp),
                        safeTrialCount(profile),
                        guestSecuritySupport.getTrialLimit()
                ));
    }

    @Transactional
    public GuestProfileContext consumeInspirationTrial(Long userId, String installationId, String clientIp, String requestId) {
        GuestProfile profile = guestProfileRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("Guest profile not found"));
        String installIdHash = guestSecuritySupport.hashInstallationId(installationId);
        String ipHash = guestSecuritySupport.hashClientIp(clientIp);

        if (!installIdHash.equals(profile.getInstallIdHash())) {
            throw new GuestAccessException(
                    HttpStatus.UNAUTHORIZED,
                    GuestSecuritySupport.ERROR_INVALID_GUEST_CONTEXT,
                    "游客上下文已失效，请重新进入体验",
                    profile.getTrialRemaining()
            );
        }

        int used = safeTrialCount(profile);
        int limit = guestSecuritySupport.getTrialLimit();
        if (used >= limit) {
            log.warn(
                    "decision={} requestId={} guestProfileId={} installIdHash={} ipHash={} trialUsedCount={} reason={}",
                    "GUEST_TRIAL_EXHAUSTED",
                    requestId,
                    profile.getId(),
                    installIdHash,
                    ipHash,
                    used,
                    "guest_trial_limit"
            );
            throw new GuestAccessException(
                    HttpStatus.FORBIDDEN,
                    GuestSecuritySupport.ERROR_GUEST_TRIAL_EXHAUSTED,
                    "游客灵感次数已用完，请登录后继续",
                    0
            );
        }

        profile.setTrialUsedCount(used + 1);
        profile.setLastSeenIpHash(ipHash);
        profile.setLastSeenAt(LocalDateTime.now());
        GuestProfile saved = guestProfileRepository.save(profile);
        return new GuestProfileContext(userId, saved.getId(), installIdHash, ipHash, safeTrialCount(saved), limit);
    }

    private GuestProfile createGuestProfile(String installIdHash, String ipHash) {
        User user = new User();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String shortCode = uuid.substring(0, 8);
        user.setUsername("guest_" + shortCode);
        user.setDisplayName("游客");
        user.setEmail("guest_" + uuid + "@guest.local");
        user.setPhone(null);
        user.setPassword(passwordEncoder.encode("guest:" + UUID.randomUUID()));
        user.setBio("");
        user.setAvatarUrl("");
        user.setGender("");
        user.setRegion("");
        user.setFailedLoginAttempts(0);
        User savedUser = userRepository.save(user);

        GuestProfile profile = new GuestProfile();
        profile.setUser(savedUser);
        profile.setInstallIdHash(installIdHash);
        profile.setFirstSeenIpHash(ipHash);
        profile.setLastSeenIpHash(ipHash);
        profile.setTrialMaxCount(guestSecuritySupport.getTrialLimit());
        profile.setTrialUsedCount(0);
        profile.setBlockedUntil(null);
        profile.setLastAuthAt(LocalDateTime.now());
        profile.setLastSeenAt(LocalDateTime.now());
        return guestProfileRepository.save(profile);
    }

    private GuestProfile touchGuestProfile(GuestProfile profile, String ipHash) {
        profile.setLastAuthAt(LocalDateTime.now());
        profile.setLastSeenIpHash(ipHash);
        profile.setLastSeenAt(LocalDateTime.now());
        return guestProfileRepository.save(profile);
    }

    private int safeTrialCount(GuestProfile profile) {
        return profile.getTrialUsedCount() == null ? 0 : profile.getTrialUsedCount();
    }

    public record GuestAuthSession(User user, Long guestProfileId, String token, int trialRemaining) {
    }
}
