package com.xzy.forum.services.impl;

import com.xzy.forum.model.Board;
import com.xzy.forum.services.IBoardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


@SpringBootTest
class BoardServiceImplTest {

    @Autowired
    private IBoardService iBoardService;
    @Test
    void selectByNum() {
        List<Board> result = iBoardService.selectByNum(6);
        System.out.println(result);
    }

    @Test
    void addOneArticleCount() {

        iBoardService.addOneArticleCount(1L);

    }
}