package com.goaway.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class GuestContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GuestContextFilter.class);
    private final JwtUtils jwtUtils;
    private final GuestSecuritySupport guestSecuritySupport;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GuestContextFilter(JwtUtils jwtUtils, GuestSecuritySupport guestSecuritySupport) {
        this.jwtUtils = jwtUtils;
        this.guestSecuritySupport = guestSecuritySupport;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !Boolean.TRUE.equals(authentication.getDetails())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = parseJwt(request);
        if (token == null) {
            writeInvalidGuestContext(request, response, null, null, "missing_token");
            return;
        }

        String installationId = request.getHeader(GuestSecuritySupport.GUEST_INSTALLATION_HEADER);
        if (!guestSecuritySupport.isValidInstallationId(installationId)) {
            writeInvalidGuestContext(request, response, token, installationId, "invalid_installation_id");
            return;
        }

        String expectedInstallHash = jwtUtils.extractGuestInstallHash(token).orElse(null);
        String actualInstallHash = guestSecuritySupport.hashInstallationId(installationId);
        if (expectedInstallHash == null || !expectedInstallHash.equals(actualInstallHash)) {
            writeInvalidGuestContext(request, response, token, actualInstallHash, "token_installation_mismatch");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    private void writeInvalidGuestContext(
            HttpServletRequest request,
            HttpServletResponse response,
            String token,
            String installOrHash,
            String reason
    ) throws IOException {
        String installIdHash = installOrHash;
        if (installIdHash != null && installIdHash.length() != 64 && guestSecuritySupport.isValidInstallationId(installOrHash)) {
            installIdHash = guestSecuritySupport.hashInstallationId(installOrHash);
        }
        log.warn(
                "decision={} requestId={} guestProfileId={} installIdHash={} ipHash={} trialUsedCount={} reason={}",
                "GUEST_TOKEN_MISMATCH",
                request.getHeader("X-Request-ID"),
                safeGuestProfileId(token),
                installIdHash,
                guestSecuritySupport.hashClientIp(guestSecuritySupport.resolveClientIp(request)),
                null,
                reason
        );
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                new GuestAccessErrorResponse(
                        GuestSecuritySupport.ERROR_INVALID_GUEST_CONTEXT,
                        "游客上下文已失效，请重新进入体验",
                        null
                )
        );
    }

    private Long safeGuestProfileId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return jwtUtils.extractGuestProfileId(token).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }
}
