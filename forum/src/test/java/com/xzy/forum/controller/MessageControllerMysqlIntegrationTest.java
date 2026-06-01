package com.xzy.forum.controller;

import com.xzy.forum.config.AppConfig;
import com.xzy.forum.model.Message;
import com.xzy.forum.model.User;
import com.xzy.forum.services.IMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Comparator;
import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@EnabledIfSystemProperty(named = "forum.mysql.it", matches = "true")
class MessageControllerMysqlIntegrationTest {

    private static final long USER_ONE_ID = 1L;
    private static final long USER_TWO_ID = 2L;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private IMessageService messageService;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldSendAndQueryMessagesViaHttp() throws Exception {
        MockHttpSession senderSession = loginSession(USER_ONE_ID);
        MockHttpSession receiverSession = loginSession(USER_TWO_ID);
        String sendContent = "http-send-" + System.nanoTime();

        mockMvc.perform(post("/message/send")
                        .session(senderSession)
                        .param("receiveUserId", String.valueOf(USER_TWO_ID))
                        .param("content", sendContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.message", is("发送成功")));

        mockMvc.perform(get("/message/getUnreadCount")
                        .session(receiverSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/message/getAll")
                        .session(receiverSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data[*].content", hasItem(sendContent)));
    }

    @Test
    void shouldMarkReadAndReplyViaHttp() throws Exception {
        String sourceContent = "http-source-" + System.nanoTime();
        messageService.send(USER_ONE_ID, USER_TWO_ID, sourceContent);
        List<Message> inbox = messageService.selectInbox(USER_TWO_ID);
        Message sourceMessage = inbox.stream()
                .filter(message -> sourceContent.equals(message.getContent()))
                .max(Comparator.comparing(Message::getId))
                .orElseThrow();

        MockHttpSession receiverSession = loginSession(USER_TWO_ID);
        MockHttpSession senderSession = loginSession(USER_ONE_ID);

        mockMvc.perform(post("/message/markRead")
                        .session(receiverSession)
                        .param("id", String.valueOf(sourceMessage.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.message", is("已标记为已读")));

        mockMvc.perform(get("/message/getUnreadCount")
                        .session(receiverSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data", is(0)));

        String replyContent = "http-reply-" + System.nanoTime();
        mockMvc.perform(post("/message/reply")
                        .session(receiverSession)
                        .param("repliedId", String.valueOf(sourceMessage.getId()))
                        .param("content", replyContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.message", is("回复成功")));

        mockMvc.perform(get("/message/getAll")
                        .session(senderSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data[*].content", hasItem(replyContent)));
    }

    private MockHttpSession loginSession(Long userId) {
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(userId);
        session.setAttribute(AppConfig.USER_SESSION, user);
        return session;
    }
}
