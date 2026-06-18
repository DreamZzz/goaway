package com.goaway.platform.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    @Test
    @DisplayName("generateGuestToken should embed guest claims with 7 day ttl")
    void generateGuestToken_ShouldEmbedGuestClaims() {
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3_600_000L);
        ReflectionTestUtils.setField(jwtUtils, "guestJwtExpirationMs", 604_800_000L);

        Instant before = Instant.now();
        String token = jwtUtils.generateGuestToken("guest_user", 77L, "install-hash");
        Date expiration = jwtUtils.extractExpiration(token);

        assertTrue(jwtUtils.isGuestToken(token));
        assertEquals(77L, jwtUtils.extractGuestProfileId(token).orElseThrow());
        assertEquals("install-hash", jwtUtils.extractGuestInstallHash(token).orElseThrow());
        assertTrue(Duration.between(before, expiration.toInstant()).toDays() >= 6);
    }
}
