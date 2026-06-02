package com.xzy.forum.utils;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpUtils {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
    };

    private ClientIpUtils() {
    }

    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        for (String header : IP_HEADERS) {
            String value = request.getHeader(header);
            if (!StringUtils.isEmpty(value) && !"unknown".equalsIgnoreCase(value)) {
                int commaIndex = value.indexOf(',');
                return commaIndex > 0 ? value.substring(0, commaIndex).trim() : value.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
