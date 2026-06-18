package com.goaway.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class GuestRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GuestRateLimitFilter.class);
    private final ConcurrentHashMap<String, Deque<Long>> guestAuthTimestamps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Long>> guestMealTimestamps = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GuestSecuritySupport guestSecuritySupport;

    @Value("${app.guest.auth.rate-limit.max-requests:5}")
    private int guestAuthMaxRequests;

    @Value("${app.guest.auth.rate-limit.window-seconds:600}")
    private int guestAuthWindowSeconds;

    @Value("${app.guest.meal.rate-limit.max-requests:12}")
    private int guestMealMaxRequests;

    @Value("${app.guest.meal.rate-limit.window-seconds:600}")
    private int guestMealWindowSeconds;

    public GuestRateLimitFilter(GuestSecuritySupport guestSecuritySupport) {
        this.guestSecuritySupport = guestSecuritySupport;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !("/api/auth/guest".equals(uri) || "/api/meals/guest-inspirations".equals(uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        String installationId = request.getHeader(GuestSecuritySupport.GUEST_INSTALLATION_HEADER);
        String installIdHash = guestSecuritySupport.isValidInstallationId(installationId)
                ? guestSecuritySupport.hashInstallationId(installationId)
                : null;
        String ipHash = guestSecuritySupport.hashClientIp(guestSecuritySupport.resolveClientIp(request));
        String normalizedIpHash = ipHash == null ? "ip:missing" : ipHash;
        String key = installIdHash != null ? installIdHash + "|" + normalizedIpHash : normalizedIpHash;
        if ("/api/auth/guest".equals(uri)) {
            if (isLimited(guestAuthTimestamps, key, guestAuthMaxRequests, guestAuthWindowSeconds)) {
                writeLimited(response, request, installIdHash, normalizedIpHash, "GUEST_RATE_LIMITED", "游客入口请求过于频繁，请稍后再试");
                return;
            }
        } else if ("/api/meals/guest-inspirations".equals(uri)) {
            if (isLimited(guestMealTimestamps, key, guestMealMaxRequests, guestMealWindowSeconds)) {
                writeLimited(response, request, installIdHash, normalizedIpHash, "GUEST_RATE_LIMITED", "游客体验次数请求过于频繁，请稍后再试");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLimited(ConcurrentHashMap<String, Deque<Long>> timestamps, String key, int maxRequests, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;
        boolean[] limited = {false};
        timestamps.compute(key, (ignored, deque) -> {
            if (deque == null) {
                deque = new ArrayDeque<>();
            }
            long cutoff = now - windowMs;
            while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
                deque.pollFirst();
            }
            if (deque.size() >= maxRequests) {
                limited[0] = true;
            } else {
                deque.addLast(now);
            }
            return deque;
        });
        return limited[0];
    }

    private void writeLimited(
            HttpServletResponse response,
            HttpServletRequest request,
            String installIdHash,
            String ipHash,
            String error,
            String message
    ) throws IOException {
        log.warn("decision={} requestId={} guestProfileId={} installIdHash={} ipHash={} trialUsedCount={} reason={}",
                "GUEST_RATE_LIMITED",
                request.getHeader("X-Request-ID"),
                null,
                installIdHash,
                ipHash,
                null,
                request.getRequestURI());
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new GuestAccessErrorResponse(error, message, null));
    }
}
