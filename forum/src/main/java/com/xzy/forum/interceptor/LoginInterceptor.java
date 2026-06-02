package com.xzy.forum.interceptor;

import com.xzy.forum.auth.AuthContext;
import com.xzy.forum.auth.JwtAuthenticationService;
import com.xzy.forum.auth.TokenRevocationService;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.config.AppConfig;
import com.xzy.forum.model.User;
import com.xzy.forum.services.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;


@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtAuthenticationService jwtAuthenticationService;
    private final IUserService userService;
    private final TokenRevocationService tokenRevocationService;

    public LoginInterceptor(JwtAuthenticationService jwtAuthenticationService,
                            IUserService userService,
                            TokenRevocationService tokenRevocationService) {
        this.jwtAuthenticationService = jwtAuthenticationService;
        this.userService = userService;
        this.tokenRevocationService = tokenRevocationService;
    }

    /**
        * 预处理方法，在请求处理之前调用
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorizationHeader = request.getHeader(jwtAuthenticationService.getAuthorizationHeaderName());
        String token = jwtAuthenticationService.resolveToken(authorizationHeader);
        if (token == null) {
            writeUnauthorized(response, "未登录或登录已过期，请重新登录");
            return false;
        }

        if (tokenRevocationService.isRevoked(token)) {
            writeUnauthorized(response, "登录状态已失效，请重新登录");
            return false;
        }

        Long userId = jwtAuthenticationService.parseUserId(token);
        User user = userService.selectById(userId);
        if (user == null) {
            writeUnauthorized(response, "用户不存在或登录状态已失效");
            return false;
        }

        jwtAuthenticationService.validateToken(token, user);
        AuthContext.setCurrentUser(user);
        request.setAttribute(AppConfig.AUTH_USER_REQUEST_ATTRIBUTE, user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // no-op
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String escapedMessage = message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"code\":" + ResultCode.FAILED_UNAUTHORIZED.getCode()
                + ",\"message\":\"" + escapedMessage + "\",\"data\":null}");
        response.getWriter().flush();

    }
}
