package com.xzy.forum.services;

import com.xzy.forum.model.Article;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IArticleService {

        /**
         * 创建文章帖子
         * @param article
         */
        @Transactional
        void create(Article article);


        List<Article> selectAll(String keyword);

        List<Article> selectAllByBoardId(Long boardId, String keyword);

        List<Article> selectAllByUserId(Long userId);


        Article selectDetailById(Long id);

    /**
     *
     * @param id     帖子id
     * @param title  帖子标题
     * @param content
     */
     void modify(Long id , String title , String content);

    /**
     * 根据id查询文章
     * @param id
     * @return
     */
     Article selectById(Long id);




     void thumbsUpById(Long id);

    /**
     * 根据id删除帖子
     * @param id
     */
    @Transactional //事务管理，其中涉及多个更新操作
     void deleteById(Long id);


    void addOneReplyCountById(Long id);
}
