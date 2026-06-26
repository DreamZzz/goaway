package com.goaway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@SpringBootApplication
@EnableScheduling
public class GoawayBackendApplication {

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    public static void main(String[] args) {
        SpringApplication.run(GoawayBackendApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] origins = parseAllowedOrigins();
                applyAllowedOrigins(registry.addMapping("/api/**"), origins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
                applyAllowedOrigins(registry.addMapping("/uploads/**"), origins)
                        .allowedMethods("GET")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
            
            @Override
            public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/uploads/images/**")
                        .addResourceLocations("file:uploads/images/")
                        .setCachePeriod(3600);
            }

            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                registry.addViewController("/").setViewName("forward:/web/index.html");
                registry.addViewController("/v2").setViewName("forward:/v2/index.html");
                registry.addViewController("/v2/").setViewName("forward:/v2/index.html");
                // App Store 必需：隐私政策与支持页可公开访问
                registry.addViewController("/privacy").setViewName("forward:/web/privacy.html");
                registry.addViewController("/support").setViewName("forward:/web/support.html");
            }
        };
    }

    private String[] parseAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split("\\s*,\\s*"))
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
    }

    private CorsRegistration applyAllowedOrigins(CorsRegistration registration, String[] origins) {
        // Spring rejects allowCredentials(true) together with allowedOrigins("*"), so switch to patterns for that opt-in case.
        if (origins.length == 1 && "*".equals(origins[0])) {
            return registration.allowedOriginPatterns("*");
        }
        return registration.allowedOrigins(origins);
    }
}
