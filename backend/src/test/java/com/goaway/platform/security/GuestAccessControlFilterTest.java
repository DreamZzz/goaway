package com.goaway.platform.security;

import com.goaway.contexts.account.application.GuestProfileContext;
import com.goaway.contexts.account.application.GuestSessionService;
import com.goaway.contexts.account.domain.User;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GuestAccessControlFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doFilterInternal should return 403 for guest subscription access")
    void doFilterInternal_ShouldRejectSubscriptionEndpoint() throws Exception {
        GuestSecuritySupport guestSecuritySupport = mock(GuestSecuritySupport.class);
        GuestSessionService guestSessionService = mock(GuestSessionService.class);
        GuestAccessControlFilter filter = new GuestAccessControlFilter(guestSecuritySupport, guestSessionService);

        User guestUser = new User();
        guestUser.setId(7L);
        guestUser.setUsername("guest_user");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(guestUser, null, List.of());
        authentication.setDetails(Boolean.TRUE);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/subscription/status");
        request.addHeader(GuestSecuritySupport.GUEST_INSTALLATION_HEADER, "550e8400-e29b-41d4-a716-446655440000");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(guestSecuritySupport.resolveClientIp(request)).thenReturn("1.1.1.1");
        when(guestSecuritySupport.hashClientIp("1.1.1.1")).thenReturn("ip-hash");
        when(guestSessionService.resolveGuestProfile(
                7L,
                "550e8400-e29b-41d4-a716-446655440000",
                "1.1.1.1"
        )).thenReturn(Optional.of(new GuestProfileContext(7L, 77L, "install-hash", "ip-hash", 1, 3)));

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("ACCOUNT_REQUIRED"));
        assertTrue(response.getContentAsString().contains("\"guestTrialRemaining\":2"));
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("doFilterInternal should allow guest voice transcription for App Review trial")
    void doFilterInternal_ShouldAllowGuestVoiceEndpoint() throws Exception {
        GuestSecuritySupport guestSecuritySupport = mock(GuestSecuritySupport.class);
        GuestSessionService guestSessionService = mock(GuestSessionService.class);
        GuestAccessControlFilter filter = new GuestAccessControlFilter(guestSecuritySupport, guestSessionService);

        User guestUser = new User();
        guestUser.setId(7L);
        guestUser.setUsername("guest_user");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(guestUser, null, List.of());
        authentication.setDetails(Boolean.TRUE);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/voice/transcriptions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(guestSecuritySupport, guestSessionService);
    }
}
