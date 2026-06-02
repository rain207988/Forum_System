package com.xzy.forum.services.impl;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.dao.ArticleMapper;
import com.xzy.forum.exception.ApplicationException;
import com.xzy.forum.model.Article;
import com.xzy.forum.model.Board;
import com.xzy.forum.services.IBoardService;
import com.xzy.forum.services.IArticleService;
import com.xzy.forum.services.IUserService;
import com.xzy.forum.utils.ServiceValidationUtils;
import com.xzy.forum.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static com.xzy.forum.utils.ServiceValidationUtils.requireAffectedOneRow;
import static com.xzy.forum.utils.ServiceValidationUtils.requirePositiveId;

@Slf4j
@Service
public class ArticleServiceImpl implements IArticleService {

    private final ArticleMapper articleMapper;
    private final IUserService userService;
    private final IBoardService boardService;

    public ArticleServiceImpl(ArticleMapper articleMapper, IUserService userService, IBoardService boardService) {
        this.articleMapper = articleMapper;
        this.userService = userService;
        this.boardService = boardService;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "articleLists", allEntries = true)
    public void create(Article article) {
        if (article == null || article.getUserId() == null || article.getBoardId() == null
                || StringUtils.isEmpty(article.getTitle()) || StringUtils.isEmpty(article.getContent())) {
            log.warn("参数不合法，article={}", article);
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        Board board = boardService.selectById(article.getBoardId());
        if (board == null || board.getDeleteState() == 1 || board.getState() != 0) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_BOARD_BANNER));
        }

        initCreateArticle(article);
        int row = articleMapper.insertSelective(article);
        if (row != 1) {
            log.warn(ResultCode.FAILED.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED));
        }

        userService.incrementArticleCountById(article.getUserId());
        boardService.incrementArticleCountById(article.getBoardId());
        log.info("{} , userId={}, boardId={}", ResultCode.SUCCESS, article.getUserId(), article.getBoardId());
    }

    @Override
    @Cacheable(cacheNames = "articleLists", key = "'all:keyword:' + (#keyword == null ? '' : #keyword)")
    public List<Article> selectAll(String keyword) {
        return articleMapper.selectAll(normalizeKeyword(keyword));
    }

    @Override
    @Cacheable(cacheNames = "articleLists", key = "'board:' + #boardId + ':keyword:' + (#keyword == null ? '' : #keyword)")
    public List<Article> selectAllByBoardId(Long boardId, String keyword) {
        requirePositiveId(boardId, ResultCode.FAILED_PARAMS_VALIDATE, "boardId");

        Board board = boardService.selectById(boardId);
        if (board == null || board.getDeleteState() == 1) {
            log.warn("{}，boardId={}", ResultCode.FAILED_BOARD_NOT_EXISTS, boardId);
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_BOARD_NOT_EXISTS));
        }

        return articleMapper.selectAllByBoardId(boardId, normalizeKeyword(keyword));
    }

    @Override
    @Cacheable(cacheNames = "articleLists", key = "'user:' + #userId")
    public List<Article> selectAllByUserId(Long userId) {
        requirePositiveId(userId, ResultCode.FAILED_PARAMS_VALIDATE, "userId");
        ServiceValidationUtils.requireNonNull(userService.selectById(userId), ResultCode.FAILED_USER_NOT_EXISTS, "userId", userId);
        return articleMapper.selectAllByUserId(userId);
    }

    @Override
    @Transactional
    public Article selectDetailById(Long id) {
        requirePositiveId(id, ResultCode.FAILED_PARAMS_VALIDATE, "articleId");

        Article article = articleMapper.selectById(id);
        if (article == null || article.getDeleteState() == 1) {
            log.warn("{}，id={}", ResultCode.FAILED_ARTICLE_NOT_EXISTS, id);
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
        }

        int row = articleMapper.incrementVisitCountById(article.getId());
        if (row != 1) {
            log.warn(ResultCode.ERROR_SERVICES.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }

        article.setVisitCount(article.getVisitCount() + 1);
        return article;
    }

    @Override
    @CacheEvict(cacheNames = "articleLists", allEntries = true)
    public void modify(Long id, String title, String content) {
        requirePositiveId(id, ResultCode.FAILED_PARAMS_VALIDATE, "articleId");
        if (StringUtils.isEmpty(title) || StringUtils.isEmpty(content)) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        Article updateArticle = new Article();
        updateArticle.setId(id);
        updateArticle.setTitle(title.trim());
        updateArticle.setContent(content);
        updateArticle.setUpdateTime(new Date());

        int row = articleMapper.updateByPrimaryKeySelective(updateArticle);
        if (row != 1) {
            log.warn(ResultCode.ERROR_SERVICES.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }
    }

    @Override
    public Article selectById(Long id) {
        requirePositiveId(id, ResultCode.FAILED_PARAMS_VALIDATE, "articleId");

        Article article = articleMapper.selectByPrimaryKey(id);
        if (article == null || article.getDeleteState() == 1) {
            return null;
        }
        return article;
    }

    @Override
    @CacheEvict(cacheNames = "articleLists", allEntries = true)
    public void thumbsUpById(Long id) {
        requirePositiveId(id, ResultCode.FAILED_PARAMS_VALIDATE, "articleId");

        Article article = articleMapper.selectByPrimaryKey(id);
        if (article == null || article.getDeleteState() == 1 || article.getState() == 1) {
            log.warn("{}，id={}", ResultCode.FAILED_ARTICLE_NOT_EXISTS, id);
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
        }

        int row = articleMapper.incrementLikeCountById(article.getId());
        if (row != 1) {
            log.warn(ResultCode.ERROR_SERVICES.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "articleLists", allEntries = true)
    public void deleteById(Long id) {
        requirePositiveId(id, ResultCode.FAILED_PARAMS_VALIDATE, "articleId");

        Article article = articleMapper.selectByPrimaryKey(id);
        article = ServiceValidationUtils.requireNonNull(article, ResultCode.FAILED_ARTICLE_NOT_EXISTS, "articleId", id);
        if (article.getDeleteState() == 1) {
            log.warn("{}，articleId={}", ResultCode.FAILED_ARTICLE_NOT_EXISTS, id);
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
        }

        Article updateArticle = new Article();
        updateArticle.setId(id);
        updateArticle.setDeleteState((byte) 1);
        updateArticle.setUpdateTime(new Date());

        int row = articleMapper.updateByPrimaryKeySelective(updateArticle);
        requireAffectedOneRow(row, ResultCode.ERROR_SERVICES, "删除帖子失败", id);

        userService.decrementArticleCountById(article.getUserId());
        boardService.decrementArticleCountById(article.getBoardId());

        log.info("帖子删除成功，articleId={}", id);
    }

    @Override
    @CacheEvict(cacheNames = "articleLists", allEntries = true)
    public void addOneReplyCountById(Long id) {
        requirePositiveId(id, ResultCode.FAILED_PARAMS_VALIDATE, "articleId");

        Article article = articleMapper.selectByPrimaryKey(id);
        if (article == null || article.getDeleteState() == 1) {
            log.warn(ResultCode.FAILED_ARTICLE_NOT_EXISTS.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
        }
        if (article.getState() == 1) {
            log.warn(ResultCode.FAILED_ARTICLE_BANNED.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_BANNED));
        }

        int row = articleMapper.incrementReplyCountById(article.getId());
        requireAffectedOneRow(row, ResultCode.FAILED, "帖子回复数更新失败", article.getId());
    }

    private void initCreateArticle(Article article) {
        article.setTitle(article.getTitle().trim());
        article.setVisitCount(0);
        article.setReplyCount(0);
        article.setLikeCount(0);
        article.setState((byte) 0);
        article.setDeleteState((byte) 0);
        Date now = new Date();
        article.setCreateTime(now);
        article.setUpdateTime(now);
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.isEmpty(keyword) ? null : keyword.trim();
    }
}
