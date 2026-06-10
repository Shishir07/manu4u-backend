package com.manu4u.tools.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Phase 0 security configuration — API key for guest /ask sessions.
 *
 * Access model:
 *   POST /ask              → requires X-Api-Key (ROLE_GUEST or higher)
 *   GET  /tools/**         → open  (dev debug endpoints — locked down in Phase 1)
 *   POST /admin/**         → open  (dev-only — locked down in Phase 1)
 *   POST /ingest/**        → open  (dev-only — locked down in Phase 1)
 *   GET  /swagger-ui/**    → open
 *   GET  /v3/api-docs/**   → open
 *   GET  /actuator/health  → open
 *
 * Phase 1 will add JWT bearer token support for signed-in users and restrict the
 * admin/ingest paths to ROLE_ADMIN.  All changes are additive — this config is
 * designed to extend cleanly.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GuestApiKeyFilter guestApiKeyFilter;
    private final ApiKeyAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // REST API — no CSRF, no server-side sessions
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Disable Spring Security's default form login and HTTP Basic prompts
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)

            // CORS — permissive for now; tighten to specific frontend origin in prod
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Use JSON 401 instead of redirect to login page
            .exceptionHandling(e -> e.authenticationEntryPoint(authenticationEntryPoint))

            // API key filter runs before Spring Security's own auth filters
            .addFilterBefore(guestApiKeyFilter, UsernamePasswordAuthenticationFilter.class)

            .authorizeHttpRequests(auth -> auth
                // Swagger + OpenAPI docs — always open
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()

                // Actuator health — open for load balancer / container probes
                .requestMatchers("/actuator/health").permitAll()

                // Agent endpoints — both require a valid API key (Phase 1+: also accepts JWT)
                .requestMatchers("/ask", "/ask/stream").authenticated()

                // Everything else open for now — Phase 1 will lock down /admin/** and /ingest/**
                .anyRequest().permitAll()
            );

        return http.build();
    }

    /**
     * CORS policy.
     * Phase 0: allow all origins so local frontend dev (any port) works without config.
     * Phase 1: replace wildcard with specific frontend origin(s) via Manu4uProperties.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*")); // tighten in prod
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
