package com.xzy.forum.services.impl;

import com.xzy.forum.model.Article;
import com.xzy.forum.services.IArticleService;
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
class ArticleServiceImplTest {

    @Autowired
    private IArticleService articleService;

    @Test
    void shouldQueryAllArticles() {
        List<Article> articles = articleService.selectAll(null);
        assertThat(articles).hasSizeGreaterThanOrEqualTo(2);
        assertThat(articles.get(0).getUser()).isNotNull();
        assertThat(articles.get(0).getBoard()).isNotNull();
    }

    @Test
    void shouldQueryArticlesByBoardId() {
        List<Article> articles = articleService.selectAllByBoardId(1L, null);
        assertThat(articles).isNotEmpty();
        assertThat(articles).allMatch(article -> article.getBoardId().equals(1L));
    }

    @Test
    void shouldQueryArticlesByUserId() {
        List<Article> articles = articleService.selectAllByUserId(1L);
        assertThat(articles).isNotEmpty();
        assertThat(articles).allMatch(article -> article.getUserId().equals(1L));
    }

    @Test
    void shouldIncreaseVisitCountWhenLoadingDetails() {
        Article article = articleService.selectDetailById(1L);
        assertThat(article).isNotNull();
        assertThat(article.getVisitCount()).isEqualTo(13);
    }
}
