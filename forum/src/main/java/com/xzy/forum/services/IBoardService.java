package com.xzy.forum.services;

import com.xzy.forum.model.Board;

import java.util.List;

public interface IBoardService {

    /**
     * 查询num条记录
     * @param num 记录数量
     * @return 版块列表
     */
    List<Board> selectByNum(Integer num);

    List<Board> selectAllNormal();

    void addOneArticleCount(Long boardId);

    Board selectById(Long id);

    /**
     *
     * 板块文章减1
     * @param id
     */
    void subOneArticleCountById(Long id);
}
