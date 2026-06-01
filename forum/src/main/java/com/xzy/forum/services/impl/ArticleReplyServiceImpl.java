package com.xzy.forum.services.impl;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.dao.ArticleReplyMapper;
import com.xzy.forum.exception.ApplicationException;
import com.xzy.forum.model.ArticleReply;
import com.xzy.forum.services.IArticleReplyService;
import com.xzy.forum.services.IArticleService;
import com.xzy.forum.utils.StringUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ArticleReplyServiceImpl implements IArticleReplyService {

    @Resource
    private ArticleReplyMapper articleReplyMapper;

    @Resource
    private IArticleService articleService;

    @Override
    public void create(ArticleReply articleReply) {
        // 非空校验
        if (articleReply == null || articleReply.getArticleId() == null
                || articleReply.getPostUserId() == null
                || StringUtils.isEmpty(articleReply.getContent())) {
            // 打印日志
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            // 抛出异常
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        // 设置默认值
        articleReply.setReplyId(null);
        articleReply.setReplyUserId(null);
        articleReply.setLikeCount(0);
        articleReply.setState((byte) 0);
        articleReply.setDeleteState((byte) 0);
        Date date = new Date();
        articleReply.setCreateTime(date);
        articleReply.setUpdateTime(date);
        // 写入数据库
        int row = articleReplyMapper.insertSelective(articleReply);
        if (row != 1) {
            // 打印日志
            log.warn(ResultCode.ERROR_SERVICES.toString());
            // 抛出异常
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }

        // 更新帖子表中的回复数
        articleService.addOneReplyCountById(articleReply.getArticleId());
        // 打印日志
        log.info("回复成功, article id = " + articleReply.getArticleId() + ", user id = " +
                articleReply.getPostUserId());

    }

    @Override
    public List<ArticleReply> selectByArticleId(Long articleId) {
        // 非空校验
        if (articleId == null || articleId <= 0) {
            // 打印日志
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            // 抛出异常
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        // 调用DAO
        List<ArticleReply> result = articleReplyMapper.selectByArticleId(articleId);
        // 返回结果
        return result;
    }
}
