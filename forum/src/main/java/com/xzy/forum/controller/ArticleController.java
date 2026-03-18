package com.xzy.forum.controller;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.model.Article;
import com.xzy.forum.model.Board;
import com.xzy.forum.model.User;
import com.xzy.forum.services.IArticleService;
import com.xzy.forum.services.IBoardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Tag(name = "文章管理", description = "文章的查询相关 API")
@RequestMapping("/articles")
public class ArticleController {

@Autowired
private IBoardService iBoardService;

@Autowired
private IArticleService iArticleService;



    @PostMapping("/create")
     public AppResult create(HttpSession session, @RequestParam("boardId") long boardId, String title, String content) {

        //校验用户是否被禁言
        User user = (User) session.getAttribute("user");
        if(user.getState() == 1){
            return AppResult.failed(ResultCode.FAILED, "您已被禁言，无法发帖");
        }
        //板块校验状态校验
        Board board = iBoardService.selectById(boardId);
        if(board == null || board.getState()!= 0){

            log.warn(ResultCode.FAILED_BOARD_BANNER.toString());
            return AppResult.failed(ResultCode.FAILED_BOARD_BANNER);
        }

        //封装文章对象
        Article article = new Article();
        article.setBoardId(boardId);
        article.setTitle(title);
        article.setContent(content);
        article.setUserId(user.getId());

        iArticleService.create(article);


        return AppResult.success(ResultCode.SUCCESS);
    }
}
