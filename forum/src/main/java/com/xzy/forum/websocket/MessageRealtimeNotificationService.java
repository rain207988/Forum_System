package com.xzy.forum.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class MessageRealtimeNotificationService {

    public static final String TYPE_MESSAGE_CREATED = "message-created";
    public static final String TYPE_MESSAGE_INBOX_REFRESH = "message-inbox-refresh";

    private final MessageWebSocketSessionRegistry sessionRegistry;

    public MessageRealtimeNotificationService(MessageWebSocketSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public void notifyMessageCreatedAfterCommit(Long userId, String reason) {
        notifyAfterCommit(userId, TYPE_MESSAGE_CREATED, reason);
    }

    public void notifyInboxRefreshAfterCommit(Long userId, String reason) {
        notifyAfterCommit(userId, TYPE_MESSAGE_INBOX_REFRESH, reason);
    }

    private void notifyAfterCommit(Long userId, String type, String reason) {
        if (userId == null) {
            return;
        }
        Runnable task = () -> sendToUser(userId, type, reason);
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    private void sendToUser(Long userId, String type, String reason) {
        List<WebSocketSession> sessions = sessionRegistry.getSessions(userId);
        if (sessions.isEmpty()) {
            return;
        }
        String payload = buildPayload(type, reason, userId);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessionRegistry.unregister(session);
                continue;
            }
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException ex) {
                log.warn("发送站内信实时通知失败，userId={}, sessionId={}, reason={}", userId, session.getId(), ex.getMessage());
                sessionRegistry.unregister(session);
                closeQuietly(session);
            }
        }
    }

    private String buildPayload(String type, String reason, Long userId) {
        return "{\"type\":\"" + escapeJson(type)
                + "\",\"reason\":\"" + escapeJson(reason)
                + "\",\"userId\":" + userId
                + ",\"timestamp\":" + System.currentTimeMillis()
                + "}";
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException ignored) {
            // ignore close exception
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
