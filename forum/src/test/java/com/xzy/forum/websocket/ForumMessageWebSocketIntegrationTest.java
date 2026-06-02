package com.xzy.forum.websocket;

import com.xzy.forum.auth.JwtAuthenticationService;
import com.xzy.forum.model.User;
import com.xzy.forum.services.IMessageService;
import com.xzy.forum.services.IUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ForumMessageWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtAuthenticationService jwtAuthenticationService;

    @Autowired
    private IUserService userService;

    @Autowired
    private IMessageService messageService;

    private WebSocketSession session;

    @AfterEach
    void tearDown() throws Exception {
        if (session != null && session.isOpen()) {
            session.close(CloseStatus.NORMAL);
        }
    }

    @Test
    void shouldPushRealtimeMessageEventAfterSend() throws Exception {
        User receiver = userService.selectById(1L);
        String token = jwtAuthenticationService.generateToken(receiver);
        BlockingQueue<String> payloads = new LinkedBlockingQueue<>();

        StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
        session = webSocketClient.execute(
                        new TestTextWebSocketHandler(payloads),
                        new WebSocketHttpHeaders(),
                        URI.create("ws://127.0.0.1:" + port + "/ws/messages?token=" + token))
                .get(5, TimeUnit.SECONDS);

        messageService.send(2L, 1L, "websocket-integration-message");

        String payload = payloads.poll(5, TimeUnit.SECONDS);
        assertThat(payload).isNotBlank();
        assertThat(payload).contains("\"type\":\"message-created\"");
        assertThat(payload).contains("\"reason\":\"send\"");
        assertThat(payload).contains("\"userId\":1");
    }

    private static class TestTextWebSocketHandler extends TextWebSocketHandler {

        private final BlockingQueue<String> payloads;

        private TestTextWebSocketHandler(BlockingQueue<String> payloads) {
            this.payloads = payloads;
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            payloads.offer(message.getPayload());
        }
    }
}
