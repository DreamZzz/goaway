package com.goaway.platform.security;

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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GuestContextFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doFilterInternal should return 401 when guest installation id mismatches token")
    void doFilterInternal_ShouldRejectInstallationMismatch() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        GuestSecuritySupport guestSecuritySupport = mock(GuestSecuritySupport.class);
        GuestContextFilter filter = new GuestContextFilter(jwtUtils, guestSecuritySupport);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("guest_user", null, List.of());
        authentication.setDetails(Boolean.TRUE);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/meals/favorites");
        request.addHeader("Authorization", "Bearer guest-token");
        request.addHeader(GuestSecuritySupport.GUEST_INSTALLATION_HEADER, "550e8400-e29b-41d4-a716-446655440000");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(guestSecuritySupport.isValidInstallationId("550e8400-e29b-41d4-a716-446655440000")).thenReturn(true);
        when(jwtUtils.extractGuestInstallHash("guest-token")).thenReturn(Optional.of("expected-hash"));
        when(guestSecuritySupport.hashInstallationId("550e8400-e29b-41d4-a716-446655440000")).thenReturn("actual-hash");
        when(guestSecuritySupport.resolveClientIp(request)).thenReturn("1.1.1.1");
        when(guestSecuritySupport.hashClientIp("1.1.1.1")).thenReturn("ip-hash");
        when(jwtUtils.extractGuestProfileId("guest-token")).thenReturn(Optional.of(77L));

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("INVALID_GUEST_CONTEXT"));
        verifyNoInteractions(filterChain);
    }
}
