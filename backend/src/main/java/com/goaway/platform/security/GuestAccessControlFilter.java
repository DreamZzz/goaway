package com.goaway.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goaway.contexts.account.application.GuestProfileContext;
import com.goaway.contexts.account.application.GuestSessionService;
import com.goaway.contexts.account.domain.User;
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
import java.util.Optional;

public class GuestAccessControlFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GuestAccessControlFilter.class);
    private final GuestSecuritySupport guestSecuritySupport;
    private final GuestSessionService guestSessionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GuestAccessControlFilter(
            GuestSecuritySupport guestSecuritySupport,
            GuestSessionService guestSessionService
    ) {
        this.guestSecuritySupport = guestSecuritySupport;
        this.guestSessionService = guestSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !Boolean.TRUE.equals(authentication.getDetails())) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        if (!isForbiddenForGuest(request.getMethod(), uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = extractUserId(authentication);
        String installationId = request.getHeader(GuestSecuritySupport.GUEST_INSTALLATION_HEADER);
        String clientIp = guestSecuritySupport.resolveClientIp(request);
        Optional<GuestProfileContext> guestContext = guestSessionService.resolveGuestProfile(userId, installationId, clientIp);
        Integer guestTrialRemaining = guestContext.map(GuestProfileContext::trialRemaining).orElse(null);
        String installIdHash = guestContext.map(GuestProfileContext::installIdHash).orElse(null);
        String ipHash = guestContext.map(GuestProfileContext::ipHash)
                .orElseGet(() -> guestSecuritySupport.hashClientIp(clientIp));
        String error = uri.startsWith("/api/subscription/") ? GuestSecuritySupport.ERROR_ACCOUNT_REQUIRED : "GUEST_RESTRICTED";
        String message = guestContext.isPresent()
                ? "登录后可使用文字/语音 AI 推荐与账号相关功能"
                : "游客上下文已失效，请重新进入体验";
        int status = guestContext.isPresent() ? HttpServletResponse.SC_FORBIDDEN : HttpServletResponse.SC_UNAUTHORIZED;

        log.warn(
                "decision={} requestId={} guestProfileId={} installIdHash={} ipHash={} trialUsedCount={} reason={}",
                "GUEST_ENDPOINT_FORBIDDEN",
                request.getHeader("X-Request-ID"),
                guestContext.map(GuestProfileContext::guestProfileId).orElse(null),
                installIdHash,
                ipHash,
                guestContext.map(GuestProfileContext::trialUsedCount).orElse(null),
                uri
        );
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new GuestAccessErrorResponse(error, message, guestTrialRemaining));
    }

    private boolean isForbiddenForGuest(String method, String uri) {
        return uri.startsWith("/api/subscription/")
                || uri.startsWith("/api/inventory/")
                || uri.startsWith("/api/users/")
                || "/api/meals/intent".equals(uri)
                || "/api/meals/recommendations".equals(uri)
                || "/api/meals/recommendations/stream".equals(uri)
                || uri.startsWith("/api/meals/recommendations/") && uri.endsWith("/feedback")
                || "/api/meals/favorites".equals(uri)
                || "/api/meals/history".equals(uri)
                || (uri.startsWith("/api/meals/requests/") && uri.endsWith("/shopping-list"))
                || ("POST".equalsIgnoreCase(method) && uri.startsWith("/api/meals/recipes/") && uri.endsWith("/steps/stream"))
                || ("POST".equalsIgnoreCase(method) && uri.startsWith("/api/meals/recipes/") && uri.endsWith("/image"))
                || ("POST".equalsIgnoreCase(method) && uri.startsWith("/api/meals/recipes/") && uri.endsWith("/video"))
                || ("PUT".equalsIgnoreCase(method) && uri.startsWith("/api/meals/recipes/") && uri.endsWith("/preference"));
    }

    private Long extractUserId(Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getId();
        }
        return null;
    }
}
