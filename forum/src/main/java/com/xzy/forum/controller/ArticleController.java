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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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
     public AppResult create(HttpSession session, @RequestParam("boardId") Long boardId, String title, String content) {

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

    /**
     *
     * @param boardId  不是一定要传的参数，如果不传就查询所有的帖子，如果传了就查询对应板块的帖子
     *                 查询所有帖子是首页的功能，查询对应板块的帖子是点击板块进入帖子列表页的功能
     * @return
     */
    //获取帖子列表，首页的功能
    @GetMapping("/board")
        public AppResult<List<Article>>   getAllByBoardId(@RequestParam(value = "boardId" , required = false) Long boardId) {

            List<Article> articleList;
            if (boardId ==  null){
                articleList = iArticleService.selectAll();
            }else {
                articleList = iArticleService.selectAllByBoardId(boardId);
            }


            //结果集合为空，new一个空对象返回即可
        /**
         * 为空代表
         */
        if(articleList == null ){
                articleList = new ArrayList<>();
            }

            //如果不为空就正常返回结果
            return AppResult.success(articleList);
        }

    /**
     * 拉去板块信息
     * @param id
     * @return
     */
    @GetMapping("getById")
        public AppResult<Board> getById(@RequestParam("id")  @NonNull Long id) {

                Board board = iBoardService.selectById(id);


                //不存在，或者已经是删除的状态
                if(board == null){
                    log.warn(ResultCode.FAILED_BOARD_NOT_EXISTS.toString() + "，boardId = " + id);
                    return AppResult.failed(ResultCode.FAILED_BOARD_NOT_EXISTS);
                }


                return AppResult.success(board);
        }



}
