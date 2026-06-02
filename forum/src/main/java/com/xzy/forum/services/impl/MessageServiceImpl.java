package com.xzy.forum.services.impl;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.dao.MessageMapper;
import com.xzy.forum.exception.ApplicationException;
import com.xzy.forum.model.Message;
import com.xzy.forum.model.User;
import com.xzy.forum.services.IMessageService;
import com.xzy.forum.services.IUserService;
import com.xzy.forum.utils.ServiceValidationUtils;
import com.xzy.forum.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class MessageServiceImpl implements IMessageService {

    private static final byte STATE_UNREAD = 0;
    private static final byte STATE_READ = 1;
    private static final byte STATE_REPLIED = 2;

    private final MessageMapper messageMapper;
    private final IUserService userService;
    private final CacheManager cacheManager;

    public MessageServiceImpl(MessageMapper messageMapper,
                              IUserService userService,
                              ObjectProvider<CacheManager> cacheManagerProvider) {
        this.messageMapper = messageMapper;
        this.userService = userService;
        this.cacheManager = cacheManagerProvider.getIfAvailable();
    }

    @Override
    @Transactional
    public void send(Long postUserId, Long receiveUserId, String content) {
        ServiceValidationUtils.requirePositiveId(postUserId, ResultCode.FAILED_PARAMS_VALIDATE, "postUserId");
        ServiceValidationUtils.requirePositiveId(receiveUserId, ResultCode.FAILED_PARAMS_VALIDATE, "receiveUserId");
        if (postUserId.equals(receiveUserId)) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_MESSAGE_SELF));
        }
        if (StringUtils.isEmpty(content)) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        User sender = ServiceValidationUtils.requireNonNull(userService.selectById(postUserId), ResultCode.FAILED_USER_NOT_EXISTS, "postUserId", postUserId);
        User receiver = ServiceValidationUtils.requireNonNull(userService.selectById(receiveUserId), ResultCode.FAILED_USER_NOT_EXISTS, "receiveUserId", receiveUserId);
        if (sender.getState() == 1 || receiver.getState() == 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_FORBIDDEN));
        }

        Message message = buildMessage(postUserId, receiveUserId, content);
        int row = messageMapper.insertSelective(message);
        ServiceValidationUtils.requireAffectedOneRow(row, ResultCode.FAILED_CREATE, "发送站内信失败", receiveUserId);
        evictUnreadCountCache(receiveUserId);
    }

    @Override
    @Transactional
    public void reply(Long currentUserId, Long repliedId, String content) {
        ServiceValidationUtils.requirePositiveId(currentUserId, ResultCode.FAILED_PARAMS_VALIDATE, "currentUserId");
        ServiceValidationUtils.requirePositiveId(repliedId, ResultCode.FAILED_PARAMS_VALIDATE, "repliedId");
        if (StringUtils.isEmpty(content)) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        Message originalMessage = ServiceValidationUtils.requireNonNull(messageMapper.selectDetailById(repliedId), ResultCode.FAILED_MESSAGE_NOT_EXISTS, "messageId", repliedId);
        if (!currentUserId.equals(originalMessage.getReceiveUserId())) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_MESSAGE_FORBIDDEN));
        }

        Message updateMessage = new Message();
        updateMessage.setId(repliedId);
        updateMessage.setState(STATE_REPLIED);
        updateMessage.setUpdateTime(new Date());
        int updatedRow = messageMapper.updateByPrimaryKeySelective(updateMessage);
        ServiceValidationUtils.requireAffectedOneRow(updatedRow, ResultCode.FAILED, "更新站内信回复状态失败", repliedId);

        send(currentUserId, originalMessage.getPostUserId(), content);
        evictUnreadCountCache(currentUserId);
    }

    @Override
    public List<Message> selectInbox(Long receiveUserId) {
        ServiceValidationUtils.requirePositiveId(receiveUserId, ResultCode.FAILED_PARAMS_VALIDATE, "receiveUserId");
        return messageMapper.selectInboxByReceiveUserId(receiveUserId);
    }

    @Override
    @Cacheable(cacheNames = "messageUnreadCounts", key = "#receiveUserId")
    public int countUnread(Long receiveUserId) {
        ServiceValidationUtils.requirePositiveId(receiveUserId, ResultCode.FAILED_PARAMS_VALIDATE, "receiveUserId");
        return messageMapper.countUnreadByReceiveUserId(receiveUserId);
    }

    @Override
    @Transactional
    public void markRead(Long messageId, Long currentUserId) {
        ServiceValidationUtils.requirePositiveId(messageId, ResultCode.FAILED_PARAMS_VALIDATE, "messageId");
        ServiceValidationUtils.requirePositiveId(currentUserId, ResultCode.FAILED_PARAMS_VALIDATE, "currentUserId");

        Message message = ServiceValidationUtils.requireNonNull(messageMapper.selectByPrimaryKey(messageId), ResultCode.FAILED_MESSAGE_NOT_EXISTS, "messageId", messageId);
        if (!currentUserId.equals(message.getReceiveUserId())) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_MESSAGE_FORBIDDEN));
        }
        if (message.getState() != STATE_UNREAD) {
            return;
        }

        int row = messageMapper.markRead(messageId, currentUserId);
        ServiceValidationUtils.requireAffectedOneRow(row, ResultCode.FAILED, "标记站内信已读失败", messageId);
        evictUnreadCountCache(currentUserId);
    }

    private Message buildMessage(Long postUserId, Long receiveUserId, String content) {
        Date now = new Date();
        Message message = new Message();
        message.setPostUserId(postUserId);
        message.setReceiveUserId(receiveUserId);
        message.setContent(content.trim());
        message.setState(STATE_UNREAD);
        message.setDeleteState((byte) 0);
        message.setCreateTime(now);
        message.setUpdateTime(now);
        return message;
    }

    private void evictUnreadCountCache(Long userId) {
        if (cacheManager == null || userId == null) {
            return;
        }
        Cache cache = cacheManager.getCache("messageUnreadCounts");
        if (cache != null) {
            cache.evict(userId);
        }
    }
}
