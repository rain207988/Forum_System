package com.xzy.forum.services.impl;

import com.xzy.forum.model.Message;
import com.xzy.forum.services.IMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@EnabledIfSystemProperty(named = "forum.mysql.it", matches = "true")
class MessageServiceMysqlIntegrationTest {

    private static final long SENDER_ID = 1L;
    private static final long RECEIVER_ID = 2L;

    @Autowired
    private IMessageService messageService;

    @Test
    void shouldSupportSendReadAndReplyOnMysql() {
        int unreadBefore = messageService.countUnread(RECEIVER_ID);

        String sendContent = "mysql-integration-send";
        messageService.send(SENDER_ID, RECEIVER_ID, sendContent);

        List<Message> receiverInboxAfterSend = messageService.selectInbox(RECEIVER_ID);
        Message sentMessage = receiverInboxAfterSend.stream()
                .filter(message -> sendContent.equals(message.getContent()))
                .max(Comparator.comparing(Message::getId))
                .orElseThrow();

        assertThat(sentMessage.getPostUserId()).isEqualTo(SENDER_ID);
        assertThat(messageService.countUnread(RECEIVER_ID)).isEqualTo(unreadBefore + 1);

        messageService.markRead(sentMessage.getId(), RECEIVER_ID);
        assertThat(messageService.countUnread(RECEIVER_ID)).isEqualTo(unreadBefore);

        String replySeedContent = "mysql-integration-reply-source";
        messageService.send(SENDER_ID, RECEIVER_ID, replySeedContent);
        Message repliedTarget = messageService.selectInbox(RECEIVER_ID).stream()
                .filter(message -> replySeedContent.equals(message.getContent()))
                .max(Comparator.comparing(Message::getId))
                .orElseThrow();

        String replyContent = "mysql-integration-reply";
        messageService.reply(RECEIVER_ID, repliedTarget.getId(), replyContent);

        List<Message> senderInboxAfterReply = messageService.selectInbox(SENDER_ID);
        assertThat(senderInboxAfterReply)
                .anyMatch(message -> replyContent.equals(message.getContent())
                        && RECEIVER_ID == message.getPostUserId()
                        && SENDER_ID == message.getReceiveUserId());
    }
}
