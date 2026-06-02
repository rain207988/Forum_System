package com.xzy.forum.services.impl;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.dao.ArticleReplyMapper;
import com.xzy.forum.exception.ApplicationException;
import com.xzy.forum.model.ArticleReply;
import com.xzy.forum.services.IArticleReplyService;
import com.xzy.forum.services.IArticleService;
import com.xzy.forum.utils.ServiceValidationUtils;
import com.xzy.forum.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ArticleReplyServiceImpl implements IArticleReplyService {

    private final ArticleReplyMapper articleReplyMapper;
    private final IArticleService articleService;

    public ArticleReplyServiceImpl(ArticleReplyMapper articleReplyMapper, IArticleService articleService) {
        this.articleReplyMapper = articleReplyMapper;
        this.articleService = articleService;
    }

    @Override
    public void create(ArticleReply articleReply) {
        if (articleReply == null || articleReply.getArticleId() == null
                || articleReply.getPostUserId() == null
                || StringUtils.isEmpty(articleReply.getContent())) {
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        articleReply.setReplyId(null);
        articleReply.setReplyUserId(null);
        articleReply.setContent(articleReply.getContent().trim());
        articleReply.setLikeCount(0);
        articleReply.setState((byte) 0);
        articleReply.setDeleteState((byte) 0);
        Date date = new Date();
        articleReply.setCreateTime(date);
        articleReply.setUpdateTime(date);

        int row = articleReplyMapper.insertSelective(articleReply);
        if (row != 1) {
            log.warn(ResultCode.ERROR_SERVICES.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }

        articleService.addOneReplyCountById(articleReply.getArticleId());
        log.info("回复成功, articleId={}, userId={}", articleReply.getArticleId(), articleReply.getPostUserId());
    }

    @Override
    public List<ArticleReply> selectByArticleId(Long articleId) {
        ServiceValidationUtils.requirePositiveId(articleId, ResultCode.FAILED_PARAMS_VALIDATE, "articleId");
        return articleReplyMapper.selectByArticleId(articleId);
    }
}
