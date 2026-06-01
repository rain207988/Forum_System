package com.xzy.forum.services.impl;

import com.xzy.forum.model.Board;
import com.xzy.forum.services.IBoardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BoardServiceImplTest {

    @Autowired
    private IBoardService boardService;

    @Test
    void shouldQueryTopBoards() {
        List<Board> boards = boardService.selectByNum(2);
        assertThat(boards).hasSize(2);
        assertThat(boards.get(0).getName()).isEqualTo("Java");
    }

    @Test
    void shouldQuerySingleBoard() {
        Board board = boardService.selectById(1L);
        assertThat(board).isNotNull();
        assertThat(board.getArticleCount()).isEqualTo(1);
    }

    @Test
    void shouldIncreaseBoardArticleCount() {
        boardService.addOneArticleCount(3L);
        Board board = boardService.selectById(3L);
        assertThat(board.getArticleCount()).isEqualTo(1);
    }
}
