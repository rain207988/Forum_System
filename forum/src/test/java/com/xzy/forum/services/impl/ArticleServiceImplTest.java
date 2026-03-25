package com.xzy.forum.services.impl;

import com.xzy.forum.model.Article;
import com.xzy.forum.services.IArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class ArticleServiceImplTest {

    @Autowired
    private IArticleService articleService;


    @Test
    void create() {

        Article article = new Article();
        article.setUserId(1l);
        article.setBoardId(1l);
        article.setTitle("test title");
        article.setContent("test content");

        articleService.create(article);
    }

    @Test
    void selectAll() {
        List<Article> articles = articleService.selectAll();
        System.out.println(articles);

    }

    @Test
    void selectAllByBoardId() {

        List<Article> articles = articleService.selectAllByBoardId(10l);
        System.out.println(articles);

    }
}