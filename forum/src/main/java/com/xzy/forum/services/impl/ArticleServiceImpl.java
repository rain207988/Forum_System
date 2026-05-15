package com.xzy.forum.services.impl;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.dao.ArticleMapper;
import com.xzy.forum.dao.BoardMapper;
import com.xzy.forum.dao.UserMapper;
import com.xzy.forum.exception.ApplicationException;
import com.xzy.forum.model.Article;
import com.xzy.forum.model.Board;
import com.xzy.forum.services.IArticleService;
import com.xzy.forum.services.IBoardService;
import com.xzy.forum.services.IUserService;
import com.xzy.forum.utils.ServiceValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;

import static com.xzy.forum.utils.ServiceValidationUtils.requireAffectedOneRow;
import static com.xzy.forum.utils.ServiceValidationUtils.requirePositiveId;

@Slf4j
@Service
public class ArticleServiceImpl implements IArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private IUserService iUserService;


    @Autowired
    private IBoardService iBoardService;
    @Autowired
    private BoardMapper boardMapper;
    @Autowired
    private IArticleService iArticleService;


    @Override
    public void create(Article article) {
        //非空校验
        if(article == null || article.getUserId() == null || article.getBoardId() == null){
            log.warn("参数不合法，article = " + article);
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        //设置一些默认值
        article.setVisitCount(0);
        article.setReplyCount(0);
        article.setLikeCount(0);
        article.setState((byte) 0);
        article.setDeleteState((byte) 0);
        Date now = new Date();
        article.setCreateTime(now);
        article.setUpdateTime(now);


        // 1. 插入文章基本信息
        int row = articleMapper.insertSelective(article);
        //检测是否插入成功
        if(row != 1){
            log.warn(ResultCode.FAILED.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED));
        }

        // 2.更新用户表中的数据
        iUserService.addOneArticleCountById(article.getUserId());

        // 3. 更新板块表中的板块数
        iBoardService.addOneArticleCount(article.getBoardId());

        log.info(ResultCode.SUCCESS.toString() + ", user id = "+ article.getUserId() + ", board id = "+ article.getBoardId());
    }


    @Override
    public List<Article> selectAll() {
        List<Article> result = articleMapper.selectAll();
        //得到的结果，谁用谁校验，此处是controller层去校验，所以service层不校验是否为空
        return result;
    }

    @Override
    public List<Article> selectAllByBoardId(Long boardId) {

        //参数的非空校验
        if(boardId == null || boardId <= 0){
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        //检测板块是否存在
        Board board = iBoardService.selectById(boardId);
        if(board == null){
            log.warn(ResultCode.FAILED_BOARD_NOT_EXISTS.toString() + "，boardId = " + boardId);
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_BOARD_NOT_EXISTS));
        }

        List<Article> articles = articleMapper.selectAllByBoardId(boardId);

        return articles;
    }

    @Override
    public Article selectDetailById(Long id) {
        //参数检验
        if(id == null || id <= 0){
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        //调用Mapper层的selectById方法
        Article article = articleMapper.selectById(id);

        if(article == null){
            log.warn(ResultCode.FAILED_ARTICLE_NOT_EXISTS.toString() + "，id = " + id);
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
        }

        //在数据库更新文章的访问次数
        Article updateArticle = new Article();
        updateArticle.setId(article.getId());
        updateArticle.setVisitCount(article.getVisitCount() + 1);

        //动态更新
        int row = articleMapper.updateByPrimaryKeySelective(updateArticle);
        if(row != 1){
            log.warn(ResultCode.ERROR_SERVICES.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }

        //返回更新后的数据对象给前端
        article.setVisitCount(updateArticle.getVisitCount());


        return article;
    }

    @Override
    public void modify(Long id, String title, String content) {

        //参数检验
        if(id == null || id <= 0){
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        //设置更新数据
        Article updateArticle = new Article();
        updateArticle.setId(id);
        updateArticle.setTitle(title);
        updateArticle.setContent(content);
        updateArticle.setUpdateTime(new Date());

        int row = articleMapper.updateByPrimaryKeySelective(updateArticle);
        if(row != 1){
            log.warn(ResultCode.ERROR_SERVICES.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }
    }

    @Override
    public Article selectById(Long id) {

        if(id == null || id <= 0){
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        Article article = articleMapper.selectByPrimaryKey(id);
        return article;
    }

    @Override
    public void thumbsUpById(Long id) {

        if(id == null || id <= 0){
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }


        Article article = articleMapper.selectByPrimaryKey(id);
        //帖子状态检测
        //被删除了，也表示不存在
        if(article == null || article.getDeleteState() ==1){
            log.warn(ResultCode.FAILED_ARTICLE_NOT_EXISTS.toString() + "，id = " + id);
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
        }

        //帖子状态检测
        if(article.getState() ==1 ){
            log.warn(ResultCode.FAILED_ARTICLE_NOT_EXISTS.toString() + "，id = " + id);
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
        }

        Article updateArticle = new Article();
        updateArticle.setId(article.getId());
        updateArticle.setLikeCount(article.getLikeCount() + 1);
        int row = articleMapper.updateByPrimaryKeySelective(updateArticle);
        if(row != 1){
            log.warn(ResultCode.ERROR_SERVICES.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }

    }

    @Override
    @Transactional
    public void deleteById(@Validated Long id) {
        requirePositiveId(id, ResultCode.FAILED_PARAMS_VALIDATE, "articleId");

        Article article = articleMapper.selectByPrimaryKey(id);
        article = ServiceValidationUtils.requireNonNull(article, ResultCode.FAILED_ARTICLE_NOT_EXISTS, "articleId", id);

        if (article.getDeleteState() == 1) {
            log.warn("{}，articleId={}", ResultCode.FAILED_ARTICLE_NOT_EXISTS, id);
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
        }

        Article updateArticle = new Article();
        initArticle(updateArticle,id);


        int row = articleMapper.updateByPrimaryKeySelective(updateArticle);
        requireAffectedOneRow(row, ResultCode.ERROR_SERVICES, "删除帖子失败", id);

        //更新用户记录中的帖子数量
        iUserService.subOneArticleCountById(article.getUserId());
        //更新板块中的帖子数量
        iBoardService.subOneArticleCountById(article.getBoardId());

        log.info("帖子删除成功，articleId={}", id);
    }

    @Override
    public void addOneReplyCountById(@RequestParam("id") Long id) {
        //做参数校验
        requirePositiveId(id,ResultCode.ERROR_IS_NULL,"回复id参数校验失败");

        Article article = articleMapper.selectByPrimaryKey(id);

        if(article == null ||  article.getDeleteState() ==1){
            log.warn(ResultCode.FAILED_ARTICLE_NOT_EXISTS.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
        }

        //校验帖子是否封帖子
        if(article.getState() ==1 ){
            log.warn(ResultCode.FAILED_ARTICLE_BANNED.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
        }

        Article updateArticle = new Article();
        updateArticle.setId(article.getId());
        updateArticle.setLikeCount(article.getReplyCount() + 1);
        updateArticle.setUpdateTime(new Date());

        int row = articleMapper.updateByPrimaryKeySelective(updateArticle);
        requireAffectedOneRow(row,ResultCode.FAILED,"帖子回复数更新失败",article.getId());

    }

    //提炼初始化
    private void initArticle(Article updateArticle,Long id) {
        updateArticle.setId(id);
        updateArticle.setDeleteState((byte) 1);
        updateArticle.setUpdateTime(new Date());
    }
}
