package com.xzy.forum.aop;

import com.xzy.forum.auth.AuthContext;
import com.xzy.forum.common.AppResult;
import com.xzy.forum.config.ForumObservabilityProperties;
import com.xzy.forum.model.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

@Slf4j
@Aspect
@Component
public class ApplicationLoggingAspect {

    private static final String TRACE_ID_KEY = "traceId";

    private final ForumObservabilityProperties observabilityProperties;

    public ApplicationLoggingAspect(ForumObservabilityProperties observabilityProperties) {
        this.observabilityProperties = observabilityProperties;
    }

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logControllerAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            log.info("[controller-access] traceId={} method={} path={} userId={} handler={} args={} result={} durationMs={}",
                    currentTraceId(),
                    currentHttpMethod(),
                    currentRequestPath(),
                    currentUserId(),
                    joinPoint.getSignature().toShortString(),
                    summarizeArguments(joinPoint),
                    summarizeValue(result),
                    elapsedMillis(startTime));
            return result;
        } catch (Throwable ex) {
            log.warn("[controller-error] traceId={} method={} path={} userId={} handler={} args={} durationMs={} errorType={} message={}",
                    currentTraceId(),
                    currentHttpMethod(),
                    currentRequestPath(),
                    currentUserId(),
                    joinPoint.getSignature().toShortString(),
                    summarizeArguments(joinPoint),
                    elapsedMillis(startTime),
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
            throw ex;
        }
    }

    @Around("execution(public * com.xzy.forum.services.impl..*(..)) || execution(public * com.xzy.forum.service..*(..))")
    public Object logServiceAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long durationMs = elapsedMillis(startTime);
            if (durationMs >= observabilityProperties.getSlowServiceThresholdMs()) {
                log.warn("[service-slow] traceId={} userId={} service={} args={} result={} durationMs={}",
                        currentTraceId(),
                        currentUserId(),
                        joinPoint.getSignature().toShortString(),
                        summarizeArguments(joinPoint),
                        summarizeValue(result),
                        durationMs);
            }
            return result;
        } catch (Throwable ex) {
            log.error("[service-error] traceId={} userId={} service={} args={} durationMs={} errorType={} message={}",
                    currentTraceId(),
                    currentUserId(),
                    joinPoint.getSignature().toShortString(),
                    summarizeArguments(joinPoint),
                    elapsedMillis(startTime),
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
            throw ex;
        }
    }

    private long elapsedMillis(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000;
    }

    private String summarizeArguments(ProceedingJoinPoint joinPoint) {
        if (!(joinPoint.getSignature() instanceof CodeSignature codeSignature)) {
            return "[]";
        }

        String[] parameterNames = codeSignature.getParameterNames();
        Object[] values = joinPoint.getArgs();
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (int i = 0; i < values.length; i++) {
            String parameterName = parameterNames != null && i < parameterNames.length ? parameterNames[i] : "arg" + i;
            joiner.add(parameterName + "=" + summarizeParameter(parameterName, values[i]));
        }
        return joiner.toString();
    }

    private String summarizeParameter(String parameterName, Object value) {
        String normalizedName = parameterName.toLowerCase(Locale.ROOT);
        if (normalizedName.contains("password")
                || normalizedName.contains("token")
                || normalizedName.contains("authorization")
                || normalizedName.contains("secret")
                || normalizedName.contains("salt")) {
            return "***";
        }
        return summarizeValue(value);
    }

    private String summarizeValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof HttpServletRequest) {
            return "<HttpServletRequest>";
        }
        if (value instanceof AppResult<?> appResult) {
            return "AppResult(code=" + appResult.getCode() + ", message=" + abbreviate(appResult.getMessage()) + ")";
        }
        if (value instanceof User user) {
            return "User(id=" + user.getId() + ", username=" + abbreviate(user.getUsername()) + ")";
        }
        if (value instanceof CharSequence charSequence) {
            return "\"" + abbreviate(charSequence.toString()) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Collection<?> collection) {
            return value.getClass().getSimpleName() + "(size=" + collection.size() + ")";
        }
        if (value instanceof Map<?, ?> map) {
            return value.getClass().getSimpleName() + "(size=" + map.size() + ")";
        }
        if (value.getClass().isArray()) {
            return value.getClass().getComponentType().getSimpleName() + "[](size=" + Array.getLength(value) + ")";
        }
        return value.getClass().getSimpleName();
    }

    private String abbreviate(String value) {
        if (value == null) {
            return "null";
        }
        return value.length() <= 80 ? value : value.substring(0, 77) + "...";
    }

    private String currentTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    private Long currentUserId() {
        User user = AuthContext.getCurrentUser();
        return user == null ? null : user.getId();
    }

    private String currentHttpMethod() {
        HttpServletRequest request = currentRequest();
        return request == null ? "-" : request.getMethod();
    }

    private String currentRequestPath() {
        HttpServletRequest request = currentRequest();
        return request == null ? "-" : request.getRequestURI();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }
}
