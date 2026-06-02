package com.xzy.forum.services.impl;

import com.xzy.forum.common.ResultCode;
import com.xzy.forum.dao.BoardMapper;
import com.xzy.forum.model.Board;
import com.xzy.forum.services.IBoardService;
import com.xzy.forum.utils.ServiceValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class BoardServiceImpl implements IBoardService {

    @Autowired
    private BoardMapper boardMapper;




    @Override
    @Cacheable(cacheNames = "boards", key = "'top:' + #num")
    public List<Board> selectByNum(Integer num) {
        ServiceValidationUtils.requireNonNegative(num, ResultCode.FAILED_PARAMS_VALIDATE, "num");
        return boardMapper.selectByNum(num);
    }


    @Override
    @Cacheable(cacheNames = "boards", key = "'all-normal'")
    public List<Board> selectAllNormal() {
        List<Board> result = boardMapper.selectAllNormal();
        return result;
    }


    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "boards", allEntries = true),
            @CacheEvict(cacheNames = "articleLists", allEntries = true)
    })
    public void addOneArticleCount(Long boardId) {
        ServiceValidationUtils.requirePositiveId(boardId, ResultCode.FAILED_PARAMS_VALIDATE, "boardId");
        ServiceValidationUtils.requireNonNull(boardMapper.selectByPrimaryKey(boardId), ResultCode.FAILED_BOARD_NOT_EXISTS, "boardId", boardId);

        int row = boardMapper.increaseArticleCountById(boardId);
        ServiceValidationUtils.requireAffectedOneRow(row, ResultCode.FAILED, "增加板块文章数失败", boardId);
    }

    @Override
    @Cacheable(cacheNames = "boards", key = "'id:' + #id", unless = "#result == null")
    public Board selectById(Long id) {
        ServiceValidationUtils.requirePositiveId(id, ResultCode.FAILED_BOARD_ARTICLE_COUNT, "boardId");
        return boardMapper.selectByPrimaryKey(id);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "boards", allEntries = true),
            @CacheEvict(cacheNames = "articleLists", allEntries = true)
    })
    public void subOneArticleCountById(Long id) {
        ServiceValidationUtils.requirePositiveId(id, ResultCode.FAILED_BOARD_ARTICLE_COUNT, "boardId");
        ServiceValidationUtils.requireNonNull(boardMapper.selectByPrimaryKey(id), ResultCode.FAILED_BOARD_NOT_EXISTS, "boardId", id);

        int row = boardMapper.decreaseArticleCountById(id);
        ServiceValidationUtils.requireAffectedOneRow(row, ResultCode.FAILED, "减少板块文章数失败", id);
    }
}
