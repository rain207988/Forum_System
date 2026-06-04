package com.xzy.forum.controller;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.auth.AuthContext;
import com.xzy.forum.model.Message;
import com.xzy.forum.model.User;
import com.xzy.forum.config.ForumRateLimitProperties;
import com.xzy.forum.service.RateLimitService;
import com.xzy.forum.services.IMessageService;
import com.xzy.forum.utils.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/message")
public class MessageController {

    private final IMessageService messageService;
    private final RateLimitService rateLimitService;
    private final ForumRateLimitProperties rateLimitProperties;

    public MessageController(IMessageService messageService,
                             RateLimitService rateLimitService,
                             ForumRateLimitProperties rateLimitProperties) {
        this.messageService = messageService;
        this.rateLimitService = rateLimitService;
        this.rateLimitProperties = rateLimitProperties;
    }

    @PostMapping("/send")
    public AppResult<Void> send(HttpServletRequest request,
                                @RequestParam("receiveUserId") Long receiveUserId,
                                @RequestParam("content") String content) {
        User currentUser = requireLoginUser();
        rateLimitService.check(
                rateLimitProperties.getMessageSend(),
                "forum:rate-limit:message-send:user:" + currentUser.getId() + ":ip:" + ClientIpUtils.resolveClientIp(request),
                "发送站内信过于频繁，请稍后再试"
        );
        messageService.send(currentUser.getId(), receiveUserId, content);
        return AppResult.success("发送成功", null);
    }

    @PostMapping("/reply")
    public AppResult<Void> reply(HttpServletRequest request,
                                 @RequestParam("repliedId") Long repliedId,
                                 @RequestParam("content") String content) {
        User currentUser = requireLoginUser();
        rateLimitService.check(
                rateLimitProperties.getMessageReply(),
                "forum:rate-limit:message-reply:user:" + currentUser.getId() + ":ip:" + ClientIpUtils.resolveClientIp(request),
                "回复站内信过于频繁，请稍后再试"
        );
        messageService.reply(currentUser.getId(), repliedId, content);
        return AppResult.success("回复成功", null);
    }

    @GetMapping("/getUnreadCount")
    public AppResult<Integer> getUnreadCount() {
        User currentUser = requireLoginUser();
        return AppResult.success(messageService.countUnread(currentUser.getId()));
    }

    @GetMapping("/getAll")
    public AppResult<List<Message>> getAll() {
        User currentUser = requireLoginUser();
        return AppResult.success(messageService.selectInbox(currentUser.getId()));
    }

    @PostMapping("/markRead")
    public AppResult<Void> markRead(@RequestParam("id") Long id) {
        User currentUser = requireLoginUser();
        messageService.markRead(id, currentUser.getId());
        return AppResult.success("已标记为已读", null);
    }

    private User requireLoginUser() {
        return AuthContext.requireCurrentUser();
    }
}
