package com.xzy.forum.services;

import com.xzy.forum.model.Message;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IMessageService {

    @Transactional
    void send(Long postUserId, Long receiveUserId, String content);

    @Transactional
    void reply(Long currentUserId, Long repliedId, String content);

    List<Message> selectInbox(Long receiveUserId);

    int countUnread(Long receiveUserId);

    @Transactional
    void markRead(Long messageId, Long currentUserId);
}
