package com.xzy.forum.dao;

import com.xzy.forum.model.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArticleMapper {
    int insert(Article row);

    int insertSelective(Article row);

    Article selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Article row);

    int updateByPrimaryKeyWithBLOBs(Article row);

    int updateByPrimaryKey(Article row);


    /**
     * 查询所有帖子列表,用于首页展示
     * @return
     */

    List<Article> selectAll(@Param("keyword") String keyword);

    /**
     * 根据帖子id查询帖子详情
     * @param id
     * @return
     */
    Article selectById(Long id);

    /**
     * 根据板块id查询所有帖子列表
     * @param boardId
     * @return
     */
    List<Article> selectAllByBoardId(@Param("boardId") Long boardId, @Param("keyword") String keyword);

    List<Article> selectAllByUserId(Long userId);
}
