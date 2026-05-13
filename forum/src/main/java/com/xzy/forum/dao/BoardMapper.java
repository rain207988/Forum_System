package com.xzy.forum.dao;

import com.xzy.forum.model.Board;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BoardMapper {
    int insert(Board row);

    int insertSelective(Board row);

    Board selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Board row);

    int updateByPrimaryKey(Board row);


    List<Board> selectAllActive();

    List<Board>selectAllNormal();

    List<Board> selectByNum(Integer num);

    int increaseArticleCountById(Long id);

    int decreaseArticleCountById(Long id);
}
