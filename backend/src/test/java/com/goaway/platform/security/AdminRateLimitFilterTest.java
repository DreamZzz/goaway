package com.goaway.platform.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AdminRateLimitFilterTest {

    @Test
    @DisplayName("doFilter should allow ten admin requests per window and reject the eleventh")
    void doFilter_ShouldAllowTenRequestsAndRejectEleventh() throws Exception {
        AdminRateLimitFilter filter = new AdminRateLimitFilter();
        ReflectionTestUtils.setField(filter, "maxRequests", 10);
        ReflectionTestUtils.setField(filter, "windowSeconds", 60);
        FilterChain filterChain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = adminRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertEquals(200, response.getStatus());
        }

        MockHttpServletRequest limitedRequest = adminRequest();
        MockHttpServletResponse limitedResponse = new MockHttpServletResponse();

        filter.doFilter(limitedRequest, limitedResponse, filterChain);

        assertEquals(429, limitedResponse.getStatus());
        assertTrue(limitedResponse.getContentAsString().contains("请求过于频繁"));
        verify(filterChain, times(10)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private MockHttpServletRequest adminRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/console/catalog-expansion/candidates");
        request.addHeader("X-Forwarded-For", "203.0.113.8");
        return request;
    }
}
