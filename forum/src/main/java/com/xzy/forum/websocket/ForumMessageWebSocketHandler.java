package com.xzy.forum.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
public class ForumMessageWebSocketHandler extends TextWebSocketHandler {

    public static final String ATTR_USER_ID = "forumWsUserId";

    private final MessageWebSocketSessionRegistry sessionRegistry;

    public ForumMessageWebSocketHandler(MessageWebSocketSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        if (userId == null) {
            closeSession(session, CloseStatus.POLICY_VIOLATION);
            return;
        }
        sessionRegistry.register(userId, session);
        log.debug("站内信 WebSocket 连接建立，userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 当前站内信实时通道只用于服务端推送，客户端消息暂不处理。
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessionRegistry.unregister(session);
        log.warn("站内信 WebSocket 传输异常，sessionId={}, reason={}", session.getId(), exception.getMessage());
        closeSession(session, CloseStatus.SERVER_ERROR);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(session);
        log.debug("站内信 WebSocket 连接关闭，sessionId={}, status={}", session.getId(), status);
    }

    private void closeSession(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception ex) {
            log.debug("关闭站内信 WebSocket 会话失败，sessionId={}, reason={}", session.getId(), ex.getMessage());
        }
    }
}
