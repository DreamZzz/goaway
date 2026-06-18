package com.goaway.platform.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verifies Apple Sign In identity tokens (JWTs signed with Apple's RSA keys).
 * Public keys are fetched from Apple's JWKS endpoint and cached in memory.
 */
@Component
public class AppleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(AppleTokenVerifier.class);
    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    @Value("${app.apple.bundle-id}")
    private String bundleId;

    private final ObjectMapper objectMapper;
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public AppleTokenVerifier(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void preloadKeys() {
        try {
            refreshKeys();
        } catch (Exception e) {
            log.warn("Apple JWKS preload failed (will retry on first request): {}", e.getMessage());
        }
    }

    /**
     * Verifies the identity token and returns its claims.
     * Throws IllegalArgumentException if the token is invalid or verification fails.
     */
    public Claims verify(String identityToken) {
        String kid = extractKid(identityToken);
        if (!keyCache.containsKey(kid)) {
            refreshKeys();
        }
        PublicKey key = keyCache.get(kid);
        if (key == null) {
            throw new IllegalArgumentException("Unknown Apple signing key id: " + kid);
        }

        Claims claims;
        try {
            @SuppressWarnings("deprecation")
            Claims parsed = Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(identityToken)
                    .getBody();
            claims = parsed;
        } catch (JwtException e) {
            throw new IllegalArgumentException("Apple identity token invalid: " + e.getMessage(), e);
        }

        if (!APPLE_ISSUER.equals(claims.getIssuer())) {
            throw new IllegalArgumentException("Invalid Apple token issuer");
        }
        Set<String> audience = claims.getAudience();
        if (audience == null || !audience.contains(bundleId)) {
            throw new IllegalArgumentException("Invalid Apple token audience");
        }
        return claims;
    }

    private String extractKid(String token) {
        try {
            String headerJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]));
            return objectMapper.readTree(headerJson).get("kid").asText();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Apple identity token header", e);
        }
    }

    private void refreshKeys() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(APPLE_JWKS_URL))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode keys = objectMapper.readTree(response.body()).get("keys");
            KeyFactory rsaFactory = KeyFactory.getInstance("RSA");
            Map<String, PublicKey> fresh = new ConcurrentHashMap<>();
            for (JsonNode k : keys) {
                BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(k.get("n").asText()));
                BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(k.get("e").asText()));
                fresh.put(k.get("kid").asText(), rsaFactory.generatePublic(new RSAPublicKeySpec(n, e)));
            }
            keyCache.clear();
            keyCache.putAll(fresh);
            log.info("Refreshed {} Apple public keys", fresh.size());
        } catch (Exception e) {
            log.error("Failed to refresh Apple public keys: {}", e.getMessage());
        }
    }
}
