package com.manu4u.tools.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a JSON 401 instead of Spring Security's default redirect-to-login-page
 * behaviour, which makes no sense for a REST/SPA setup.
 *
 * Response body:
 * <pre>
 * { "status": 401, "error": "Unauthorized",
 *   "message": "Missing or invalid API key. Include X-Api-Key header." }
 * </pre>
 */
@Component
public class ApiKeyAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"status":401,"error":"Unauthorized",\
                "message":"Missing or invalid API key. Include X-Api-Key header."}""");
    }
}
