package com.xzy.forum.filter;

import com.xzy.forum.config.ForumSecurityHeadersProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final ForumSecurityHeadersProperties securityHeadersProperties;

    public SecurityHeadersFilter(ForumSecurityHeadersProperties securityHeadersProperties) {
        this.securityHeadersProperties = securityHeadersProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (securityHeadersProperties.isHeadersEnabled()) {
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
            response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
            response.setHeader("Content-Security-Policy", securityHeadersProperties.getContentSecurityPolicy());
            if (request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"))) {
                response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            }
        }
        filterChain.doFilter(request, response);
    }
}
