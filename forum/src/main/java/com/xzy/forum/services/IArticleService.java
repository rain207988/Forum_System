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
}
