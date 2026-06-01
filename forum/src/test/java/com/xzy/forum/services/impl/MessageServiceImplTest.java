package com.xzy.forum.services.impl;

import com.xzy.forum.model.Message;
import com.xzy.forum.services.IMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MessageServiceImplTest {

    @Autowired
    private IMessageService messageService;

    @Test
    void shouldQueryInboxAndUnreadCount() {
        List<Message> inbox = messageService.selectInbox(1L);
        assertThat(inbox).isNotEmpty();
        assertThat(inbox.get(0).getPostUser()).isNotNull();
        assertThat(messageService.countUnread(1L)).isEqualTo(1);
    }

    @Test
    void shouldMarkMessageAsRead() {
        messageService.markRead(1L, 1L);
        assertThat(messageService.countUnread(1L)).isZero();
    }

    @Test
    void shouldReplyToMessage() {
        messageService.reply(1L, 1L, "新版块需求我已经收到了。");
        List<Message> inbox = messageService.selectInbox(2L);
        assertThat(inbox).anyMatch(message -> "新版块需求我已经收到了。".equals(message.getContent()));
    }
}
