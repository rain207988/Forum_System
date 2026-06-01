package com.xzy.forum.controller;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.config.AppConfig;
import com.xzy.forum.model.Message;
import com.xzy.forum.model.User;
import com.xzy.forum.services.IMessageService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private IMessageService messageService;

    @PostMapping("/send")
    public AppResult send(HttpSession session,
                          @RequestParam("receiveUserId") Long receiveUserId,
                          @RequestParam("content") String content) {
        User currentUser = requireLoginUser(session);
        messageService.send(currentUser.getId(), receiveUserId, content);
        return AppResult.success("发送成功", null);
    }

    @PostMapping("/reply")
    public AppResult reply(HttpSession session,
                           @RequestParam("repliedId") Long repliedId,
                           @RequestParam("content") String content) {
        User currentUser = requireLoginUser(session);
        messageService.reply(currentUser.getId(), repliedId, content);
        return AppResult.success("回复成功", null);
    }

    @GetMapping("/getUnreadCount")
    public AppResult<Integer> getUnreadCount(HttpSession session) {
        User currentUser = requireLoginUser(session);
        return AppResult.success(messageService.countUnread(currentUser.getId()));
    }

    @GetMapping("/getAll")
    public AppResult<List<Message>> getAll(HttpSession session) {
        User currentUser = requireLoginUser(session);
        return AppResult.success(messageService.selectInbox(currentUser.getId()));
    }

    @PostMapping("/markRead")
    public AppResult markRead(HttpSession session, @RequestParam("id") Long id) {
        User currentUser = requireLoginUser(session);
        messageService.markRead(id, currentUser.getId());
        return AppResult.success("已标记为已读", null);
    }

    private User requireLoginUser(HttpSession session) {
        if (session == null || session.getAttribute(AppConfig.USER_SESSION) == null) {
            log.warn("用户未登录，无法访问站内信功能");
            throw new IllegalArgumentException(ResultCode.FAILED_FORBIDDEN.getMessage());
        }
        return (User) session.getAttribute(AppConfig.USER_SESSION);
    }
}
