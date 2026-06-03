package com.xzy.forum.filter;

import com.xzy.forum.config.AppConfig;
import com.xzy.forum.config.ForumObservabilityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";

    private final ForumObservabilityProperties observabilityProperties;

    public TraceIdFilter(ForumObservabilityProperties observabilityProperties) {
        this.observabilityProperties = observabilityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!observabilityProperties.isRequestTraceEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerName = observabilityProperties.getTraceHeaderName();
        String traceId = request.getHeader(headerName);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        MDC.put(TRACE_ID_KEY, traceId);
        request.setAttribute(AppConfig.TRACE_ID_REQUEST_ATTRIBUTE, traceId);
        response.setHeader(headerName, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
