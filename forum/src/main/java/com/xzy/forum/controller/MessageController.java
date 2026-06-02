package com.xzy.forum.controller;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.auth.AuthContext;
import com.xzy.forum.model.Message;
import com.xzy.forum.model.User;
import com.xzy.forum.services.IMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private IMessageService messageService;

    @PostMapping("/send")
    public AppResult send(@RequestParam("receiveUserId") Long receiveUserId,
                          @RequestParam("content") String content) {
        User currentUser = requireLoginUser();
        messageService.send(currentUser.getId(), receiveUserId, content);
        return AppResult.success("发送成功", null);
    }

    @PostMapping("/reply")
    public AppResult reply(@RequestParam("repliedId") Long repliedId,
                           @RequestParam("content") String content) {
        User currentUser = requireLoginUser();
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
    public AppResult markRead(@RequestParam("id") Long id) {
        User currentUser = requireLoginUser();
        messageService.markRead(id, currentUser.getId());
        return AppResult.success("已标记为已读", null);
    }

    private User requireLoginUser() {
        return AuthContext.requireCurrentUser();
    }
}
