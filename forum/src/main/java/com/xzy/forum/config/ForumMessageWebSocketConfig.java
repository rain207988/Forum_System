package com.xzy.forum.config;

import com.xzy.forum.websocket.AuthenticatedUserHandshakeInterceptor;
import com.xzy.forum.websocket.ForumMessageWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class ForumMessageWebSocketConfig implements WebSocketConfigurer {

    private final ForumMessageWebSocketHandler forumMessageWebSocketHandler;
    private final AuthenticatedUserHandshakeInterceptor authenticatedUserHandshakeInterceptor;
    private final ForumCorsProperties corsProperties;

    public ForumMessageWebSocketConfig(ForumMessageWebSocketHandler forumMessageWebSocketHandler,
                                       AuthenticatedUserHandshakeInterceptor authenticatedUserHandshakeInterceptor,
                                       ForumCorsProperties corsProperties) {
        this.forumMessageWebSocketHandler = forumMessageWebSocketHandler;
        this.authenticatedUserHandshakeInterceptor = authenticatedUserHandshakeInterceptor;
        this.corsProperties = corsProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(forumMessageWebSocketHandler, "/ws/messages")
                .addInterceptors(authenticatedUserHandshakeInterceptor)
                .setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns().toArray(String[]::new));
    }
}
