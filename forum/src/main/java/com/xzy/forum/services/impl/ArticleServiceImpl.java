package com.xzy.forum.services.impl;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.dao.ArticleMapper;
import com.xzy.forum.dao.BoardMapper;
import com.xzy.forum.dao.UserMapper;
import com.xzy.forum.exception.ApplicationException;
import com.xzy.forum.model.Article;
import com.xzy.forum.services.IArticleService;
import com.xzy.forum.services.IBoardService;
import com.xzy.forum.services.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

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
}
