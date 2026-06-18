package com.goaway.platform.security;


import com.goaway.contexts.account.application.GuestSessionService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;
    private final GuestSecuritySupport guestSecuritySupport;

    public SecurityConfig(
            UserDetailsService userDetailsService,
            JwtUtils jwtUtils,
            GuestSecuritySupport guestSecuritySupport) {
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
        this.guestSecuritySupport = guestSecuritySupport;
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtils, userDetailsService);
    }

    @Bean
    public GuestContextFilter guestContextFilter() {
        return new GuestContextFilter(jwtUtils, guestSecuritySupport);
    }

    @Bean
    public GuestAccessControlFilter guestAccessControlFilter(GuestSessionService guestSessionService) {
        return new GuestAccessControlFilter(guestSecuritySupport, guestSessionService);
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, GuestAccessControlFilter guestAccessControlFilter) throws Exception {
        http
            .cors(cors -> {})
            // CSRF is intentionally disabled: this is a stateless JWT API consumed by a
            // native app, not cookie-based sessions. No browser form → no CSRF surface.
            .csrf(csrf -> csrf.disable())
            // Defense-in-depth response headers to harden any HTML that does slip through
            // (error pages, static index.html, docs) and to mitigate clickjacking / MIME
            // sniffing attacks on clients that happen to render responses.
            .headers(headers -> headers
                .contentTypeOptions(opts -> {})
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(ref -> ref.policy(
                        org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000))
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // SSE endpoints use async redispatch after the initial authenticated request.
                // The controller captures user identity before opening the emitter, so the
                // async continuation itself should not be blocked by a fresh auth check.
                .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/", "/index.html", "/favicon.ico", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/v2", "/v2/", "/v2/**").permitAll()
                .requestMatchers(HttpMethod.HEAD, "/v2", "/v2/", "/v2/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.HEAD, "/actuator/health").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/analytics/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/leaderboard/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/roleplay/personas").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/soup/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                .requestMatchers("/admin/**").permitAll()
                .anyRequest().authenticated()
            );
        
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(guestContextFilter(), JwtAuthenticationFilter.class);
        http.addFilterAfter(guestAccessControlFilter, GuestContextFilter.class);
        
        return http.build();
    }
}
