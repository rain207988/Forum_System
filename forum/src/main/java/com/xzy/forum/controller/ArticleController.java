package com.xzy.forum.controller;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.model.Article;
import com.xzy.forum.model.Board;
import com.xzy.forum.model.User;
import com.xzy.forum.services.IArticleService;
import com.xzy.forum.services.IBoardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

    /**
     * 获取文章详情
     * @param id 文章 ID
     * @return 文章详情对象，如果不存在则返回错误信息
     */
    @GetMapping("/details")
    public AppResult<Article> getDetails(HttpServletRequest request, @RequestParam("id") @NonNull Long id) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");



        Article article = iArticleService.selectDetailById(id);
        //service层已经校验了文章是否存在，所以这里不需要再校验了，直接返回结果即可

        if(user.getId() == article.getUserId()){
            // 是自己的文章，设置 own 属性为 true，表示可以进行编辑和删除操作
            article.setOwn(true);
        } else {
            // 不是自己的文章，设置 own 属性为 false，表示不能进行编辑和删除操作
            article.setOwn(false);
        }
        return AppResult.success(article);
    }

    /**
     * 修改文章
     * @param request 获取用户信息 以及登录状态
     * @param id
     * @param title
     * @param content
     * @return
     */
    @PostMapping("/modify")
    public  AppResult modify(HttpServletRequest request,
                             @RequestParam("id") Long id ,
                             @RequestParam("title")  String title ,
                             @RequestParam("content") String content){

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");

        //判断用户是否被禁言
        if(user.getState() == 1){
            return AppResult.failed(ResultCode.FAILED_USER_BANNED.toString());

        }

        Article article = iArticleService.selectById(id);
        //检验帖子是否存在
        if(article == null){
            return AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS);
        }

        //检验是否是自己的帖子
        if(article.getUserId() != user.getId()){
            return AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_OWN);
        }

        //检测帖子是否被删除,或者截止了，即任何人修改该帖子
        if(article.getState() == 1){
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }


        //调用service层的修改方法
        iArticleService.modify(id, title, content);


        log.info("帖子更新成功，id = {}", id);

        //返回正确的结果
        return AppResult.success();
    }


    /**
     * 点赞
     * @param request
     * @param id
     * @return
     */

    @PostMapping("/thumbsUp")
    public AppResult thumbsUp (HttpServletRequest request, @RequestParam("id") @NonNull Long id){

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");


        //检测用户是否被禁言
        if(user.getState() == 1){
            log.warn(ResultCode.FAILED_USER_BANNED.toString());
            return AppResult.failed(ResultCode.FAILED_USER_BANNED);
        }

        iArticleService.thumbsUpById(id);

        return AppResult.success();
    }
}
