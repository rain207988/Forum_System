package com.xzy.forum.controller;


import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.exception.ApplicationException;
import com.xzy.forum.model.Board;
import com.xzy.forum.services.IBoardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/board")
public class BoardController {

    @Value("${forum.index.board.num:9}")
    private Integer indexBoardNum;


    @Autowired
    private IBoardService boardService;

    @GetMapping("/toplist")
    public AppResult<List<Board>> getTopList() {

        List<Board> boards = boardService.selectByNum(indexBoardNum);
        return AppResult.success(boards);
    }


// 指定接⼝URL映射
    @GetMapping("/allNormal")
    public AppResult<List<Board>> allNormal () {
        // 调⽤Service层获取版块信息
        List<Board> boards = boardService.selectAllNormal();
        // 返回结果
        return AppResult.success(boards);
    }


    @GetMapping("getById")
    public AppResult<Board> getById( @RequestParam("id") Long id) {
        Board board = boardService.selectById(id);

        if(board == null){
            log.warn(ResultCode.FAILED_BOARD_NOT_EXISTS.toString()+"id = " + id);
            throw  new ApplicationException(AppResult.failed(ResultCode.FAILED_BOARD_NOT_EXISTS));
        }


        return AppResult.success(board);
    }
}
