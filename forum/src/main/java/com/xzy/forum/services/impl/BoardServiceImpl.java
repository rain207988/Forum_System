package com.xzy.forum.services.impl;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.dao.BoardMapper;
import com.xzy.forum.exception.ApplicationException;
import com.xzy.forum.model.Board;
import com.xzy.forum.services.IBoardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class BoardServiceImpl implements IBoardService {

    @Autowired
    private BoardMapper boardMapper;




    @Override
    public List<Board> selectByNum(Integer num){
        if(num < 0){
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
            //throw new ApplicationException(new AppResult(ResultCode.FAILED_PARAMS_VALIDATE.getCode(),ResultCode.FAILED_PARAMS_VALIDATE.getMessage() ));
        }


        List<Board> result = boardMapper.selectByNum(num);

        //是否为空使用方去调用
        return result;


    }


    @Override
    public List<Board> selectAllNormal() {
        List<Board> result = boardMapper.selectAllNormal();
        return result;
    }


    @Override
    @Transactional
    public void addOneArticleCount(Long boardId) {
        if (boardId == null || boardId <= 0) {
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString() + "，boardId = " + boardId);
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        Board board = boardMapper.selectByPrimaryKey(boardId);
        if(board  == null){
            log.warn(ResultCode.ERROR_IS_NULL.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_IS_NULL));
        }

        //更新帖子数量
        Board updateBoard = new Board();
        updateBoard.setId(boardId);
        updateBoard.setArticleCount(board.getArticleCount()+1);

        //调用DAO. 执行更新
        Integer row = boardMapper.updateByPrimaryKeySelective(updateBoard);


        if(row != 1){
            log.warn(ResultCode.FAILED.toString() + "收影响的行数不等于1");

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED));
        }
    }

    @Override
    public Board selectById(Long id){
        if(id == null || id <= 0){
            log.warn(ResultCode.FAILED_BOARD_ARTICLE_COUNT.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_BOARD_ARTICLE_COUNT));
        }

        Board board = boardMapper.selectByPrimaryKey(id);
        return board;
    }
}
