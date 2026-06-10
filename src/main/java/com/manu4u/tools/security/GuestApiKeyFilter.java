package com.manu4u.tools.security;

import com.manu4u.tools.config.Manu4uProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Phase 0 — guest API key authentication.
 *
 * Reads the {@code X-Api-Key} header on every request. If the value matches the
 * configured {@code manu4u.guest-api-key}, it sets a ROLE_GUEST authentication in
 * the SecurityContext so Spring Security's authorisation layer sees an authenticated
 * principal.
 *
 * The filter deliberately does NOT reject unknown keys here — it simply skips setting
 * authentication and lets the downstream SecurityConfig decide whether the endpoint
 * requires authentication (returning 401 via ApiKeyAuthenticationEntryPoint).
 *
 * Phase 1+ will extend this to also accept JWT Bearer tokens for signed-in users.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuestApiKeyFilter extends OncePerRequestFilter {

    static final String API_KEY_HEADER = "X-Api-Key";

    private final Manu4uProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String providedKey = request.getHeader(API_KEY_HEADER);

        if (StringUtils.hasText(providedKey)
                && StringUtils.hasText(properties.getGuestApiKey())
                && providedKey.equals(properties.getGuestApiKey())) {

            var auth = new UsernamePasswordAuthenticationToken(
                    "guest",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_GUEST"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Guest API key authenticated for {}", request.getRequestURI());
        }

        chain.doFilter(request, response);
    }
}
