package com.xzy.forum.controller;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.config.AppConfig;
import com.xzy.forum.model.Article;
import com.xzy.forum.model.Board;
import com.xzy.forum.model.User;
import com.xzy.forum.services.IArticleService;
import com.xzy.forum.services.IBoardService;
import com.xzy.forum.utils.StringUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@Tag(name = "文章管理", description = "文章的查询相关 API")
@RequestMapping({"/article", "/articles"})
public class ArticleController {

    @Autowired
    private IBoardService boardService;

    @Autowired
    private IArticleService articleService;

    @PostMapping("/create")
    public AppResult create(HttpSession session,
                            @RequestParam("boardId") Long boardId,
                            @RequestParam("title") String title,
                            @RequestParam("content") String content) {
        User user = requireLoginUser(session);
        if (user.getState() == 1) {
            return AppResult.failed(ResultCode.FAILED_USER_BANNED);
        }

        Board board = boardService.selectById(boardId);
        if (board == null || board.getState() != 0) {
            log.warn(ResultCode.FAILED_BOARD_BANNER.toString());
            return AppResult.failed(ResultCode.FAILED_BOARD_BANNER);
        }

        Article article = new Article();
        article.setBoardId(boardId);
        article.setTitle(title);
        article.setContent(content);
        article.setUserId(user.getId());
        articleService.create(article);
        return AppResult.success("发帖成功", null);
    }

    @GetMapping("/getAllByBoardId")
    public AppResult<List<Article>> getAllByBoardId(@RequestParam(value = "boardId", required = false) Long boardId,
                                                    @RequestParam(value = "keyword", required = false) String keyword) {
        List<Article> articleList;
        if (boardId == null) {
            articleList = articleService.selectAll(keyword);
        } else {
            articleList = articleService.selectAllByBoardId(boardId, keyword);
        }

        if (articleList == null) {
            articleList = new ArrayList<>();
        }
        return AppResult.success(articleList);
    }

    @GetMapping("/getAllByUserId")
    public AppResult<List<Article>> getAllByUserId(HttpSession session,
                                                   @RequestParam(value = "userId", required = false) Long userId) {
        if (userId == null) {
            userId = requireLoginUser(session).getId();
        }
        List<Article> articles = articleService.selectAllByUserId(userId);
        return AppResult.success(articles);
    }

    @GetMapping("/details")
    public AppResult<Article> getDetails(HttpSession session, @RequestParam("id") @NonNull Long id) {
        User user = requireLoginUser(session);
        Article article = articleService.selectDetailById(id);
        article.setOwn(article.getUserId().equals(user.getId()));
        return AppResult.success(article);
    }

    @PostMapping("/modify")
    public AppResult modify(HttpSession session,
                            @RequestParam("id") Long id,
                            @RequestParam("title") String title,
                            @RequestParam("content") String content) {
        User user = requireLoginUser(session);
        if (user.getState() == 1) {
            return AppResult.failed(ResultCode.FAILED_USER_BANNED);
        }
        if (StringUtils.isEmpty(title) || StringUtils.isEmpty(content)) {
            return AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        Article article = articleService.selectById(id);
        if (article == null) {
            return AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS);
        }
        if (!article.getUserId().equals(user.getId())) {
            return AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_OWN);
        }
        if (article.getState() == 1) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }

        articleService.modify(id, title, content);
        log.info("帖子更新成功，id={}", id);
        return AppResult.success("修改成功", null);
    }

    @PostMapping("/thumbsUp")
    public AppResult thumbsUp(HttpSession session, @RequestParam("id") @NonNull Long id) {
        User user = requireLoginUser(session);
        if (user.getState() == 1) {
            log.warn(ResultCode.FAILED_USER_BANNED.toString());
            return AppResult.failed(ResultCode.FAILED_USER_BANNED);
        }

        articleService.thumbsUpById(id);
        return AppResult.success("点赞成功", null);
    }

    @PostMapping("/delete")
    public AppResult deleteById(HttpSession session, @RequestParam("id") Long id) {
        User user = requireLoginUser(session);
        Article article = articleService.selectById(id);

        if (user.getState() == 1) {
            return AppResult.failed(ResultCode.FAILED_USER_BANNED);
        }
        if (article == null) {
            return AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS);
        }
        if (!user.getId().equals(article.getUserId())) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }

        articleService.deleteById(id);
        return AppResult.success("删除成功", null);
    }

    private User requireLoginUser(HttpSession session) {
        if (session == null || session.getAttribute(AppConfig.USER_SESSION) == null) {
            throw new IllegalArgumentException(ResultCode.FAILED_FORBIDDEN.getMessage());
        }
        return (User) session.getAttribute(AppConfig.USER_SESSION);
    }
}
