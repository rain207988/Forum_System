package com.xzy.forum.services;

import com.xzy.forum.model.Article;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface IArticleService {

        /**
         * 创建文章帖子
         * @param article
         */
        @Transactional
        void create(Article article);


        List<Article> selectAll();

        List<Article> selectAllByBoardId(Long boardId);


        Article selectDetailById(@RequestParam("id") Long id);

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
     void deleteById(Long id);
}
