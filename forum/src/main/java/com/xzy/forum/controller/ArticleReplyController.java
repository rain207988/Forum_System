package com.xzy.forum.controller;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.config.AppConfig;
import com.xzy.forum.model.Article;
import com.xzy.forum.model.ArticleReply;
import com.xzy.forum.model.User;
import com.xzy.forum.services.IArticleReplyService;
import com.xzy.forum.services.IArticleService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reply")
public class ArticleReplyController {

    @Resource
    private IArticleService articleService;

    @Resource
    private IArticleReplyService articleReplyService;

    @PostMapping("/create")
    public AppResult create(HttpSession session,
                            @RequestParam("articleId") @NonNull Long articleId,
                            @RequestParam("content") @NonNull String content) {
        User user = requireLoginUser(session);
        if (user.getState() == 1) {
            return AppResult.failed(ResultCode.FAILED_USER_BANNED);
        }

        Article article = articleService.selectById(articleId);
        if (article == null || article.getDeleteState() == 1) {
            return AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS);
        }
        if (article.getState() == 1) {
            return AppResult.failed(ResultCode.FAILED_ARTICLE_BANNED);
        }

        ArticleReply articleReply = new ArticleReply();
        articleReply.setArticleId(articleId);
        articleReply.setPostUserId(user.getId());
        articleReply.setContent(content);
        articleReplyService.create(articleReply);
        return AppResult.success("回复成功", null);
    }

    @GetMapping("/getReplies")
    public AppResult<List<ArticleReply>> getRepliesByArticleId(@RequestParam("articleId") @NonNull Long articleId) {
        Article article = articleService.selectById(articleId);
        if (article == null || article.getDeleteState() == 1) {
            return AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS);
        }
        List<ArticleReply> articleReplies = articleReplyService.selectByArticleId(articleId);
        return AppResult.success(articleReplies);
    }

    private User requireLoginUser(HttpSession session) {
        if (session == null || session.getAttribute(AppConfig.USER_SESSION) == null) {
            throw new IllegalArgumentException(ResultCode.FAILED_FORBIDDEN.getMessage());
        }
        return (User) session.getAttribute(AppConfig.USER_SESSION);
    }
}
