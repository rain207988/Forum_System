package com.xzy.forum.websocket;

import com.xzy.forum.auth.JwtAuthenticationService;
import com.xzy.forum.auth.TokenRevocationService;
import com.xzy.forum.exception.ApplicationException;
import com.xzy.forum.model.User;
import com.xzy.forum.services.IUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class AuthenticatedUserHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtAuthenticationService jwtAuthenticationService;
    private final IUserService userService;
    private final TokenRevocationService tokenRevocationService;

    public AuthenticatedUserHandshakeInterceptor(JwtAuthenticationService jwtAuthenticationService,
                                                 IUserService userService,
                                                 TokenRevocationService tokenRevocationService) {
        this.jwtAuthenticationService = jwtAuthenticationService;
        this.userService = userService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            if (tokenRevocationService.isRevoked(token)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            Long userId = jwtAuthenticationService.parseUserId(token);
            User user = userService.selectById(userId);
            if (user == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            jwtAuthenticationService.validateToken(token, user);
            attributes.put(ForumMessageWebSocketHandler.ATTR_USER_ID, userId);
            return true;
        } catch (ApplicationException ex) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }
}
