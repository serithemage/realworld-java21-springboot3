package io.zhc1.realworld.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import io.zhc1.realworld.model.Article;
import io.zhc1.realworld.model.User;

@DataJpaTest
@DisplayName("ArticleJpaRepository - JPA Article Repository Tests")
class ArticleJpaRepositoryTest {
    @Autowired
    TestEntityManager entityManager;

    @Autowired
    ArticleJpaRepository articleJpaRepository;

    private User testAuthor;

    @BeforeEach
    void setUp() {
        testAuthor = new User("author@example.com", "author", "password123");
        entityManager.persist(testAuthor);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should save and find article by ID")
    void whenSaveArticle_thenCanFindById() {
        // given
        Article article = new Article(testAuthor, "Test Article", "Test description", "Test body");

        // when
        Article savedArticle = articleJpaRepository.save(article);
        entityManager.flush();
        entityManager.clear();

        // then
        Article foundArticle =
                articleJpaRepository.findById(savedArticle.getId()).orElseThrow();
        assertThat(foundArticle.getTitle()).isEqualTo("Test Article");
        assertThat(foundArticle.getSlug()).isEqualTo("test-article");
        assertThat(foundArticle.getAuthor().getUsername()).isEqualTo("author");
    }

    @Test
    @DisplayName("Should find article by slug")
    void whenArticleExists_thenFindBySlug() {
        // given
        Article article = new Article(testAuthor, "Test Article", "Test description", "Test body");
        articleJpaRepository.save(article);
        entityManager.flush();
        entityManager.clear();

        // when
        Article foundArticle = articleJpaRepository.findBySlug("test-article").orElseThrow();

        // then
        assertThat(foundArticle.getTitle()).isEqualTo("Test Article");
        assertThat(foundArticle.getDescription()).isEqualTo("Test description");
    }

    @Test
    @DisplayName("Should return empty when article not found by slug")
    void whenArticleDoesNotExist_thenFindBySlugReturnsEmpty() {
        // when
        var result = articleJpaRepository.findBySlug("nonexistent-slug");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should check if article exists by title")
    void whenArticleExists_thenExistsByTitleReturnsTrue() {
        // given
        Article article = new Article(testAuthor, "Test Article", "Test description", "Test body");
        articleJpaRepository.save(article);
        entityManager.flush();

        // when & then
        assertThat(articleJpaRepository.existsByTitle("Test Article")).isTrue();
        assertThat(articleJpaRepository.existsByTitle("Nonexistent Article"))
                .isFalse();
    }

    @Test
    @DisplayName("Should find articles by author with pagination")
    void whenArticlesExist_thenFindByAuthorInOrderByCreatedAtDesc() {
        // given
        Article article1 = new Article(testAuthor, "First Article", "Description 1", "Body 1");
        Article article2 = new Article(testAuthor, "Second Article", "Description 2", "Body 2");
        articleJpaRepository.save(article1);
        articleJpaRepository.save(article2);
        entityManager.flush();
        entityManager.clear();

        // when
        Page<Article> articles = articleJpaRepository.findByAuthorInOrderByCreatedAtDesc(
                List.of(testAuthor), PageRequest.of(0, 10));

        // then
        assertThat(articles.getContent()).hasSize(2);
        assertThat(articles.getContent().get(0).getTitle()).isEqualTo("Second Article");
        assertThat(articles.getContent().get(1).getTitle()).isEqualTo("First Article");
    }

    @Test
    @DisplayName("Should find all articles with pagination")
    void whenArticlesExist_thenFindAllWithPagination() {
        // given
        Article article1 = new Article(testAuthor, "First Article", "Description 1", "Body 1");
        Article article2 = new Article(testAuthor, "Second Article", "Description 2", "Body 2");
        Article article3 = new Article(testAuthor, "Third Article", "Description 3", "Body 3");
        articleJpaRepository.saveAll(List.of(article1, article2, article3));
        entityManager.flush();

        // when
        Page<Article> firstPage = articleJpaRepository.findAll(PageRequest.of(0, 2));
        Page<Article> secondPage = articleJpaRepository.findAll(PageRequest.of(1, 2));

        // then
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should update article details")
    void whenUpdateArticle_thenChangesArePersisted() {
        // given
        Article article = new Article(testAuthor, "Test Article", "Test description", "Test body");
        Article savedArticle = articleJpaRepository.save(article);
        entityManager.flush();
        entityManager.clear();

        // when
        savedArticle.setTitle("Updated Title");
        savedArticle.setDescription("Updated description");
        savedArticle.setContent("Updated body");
        articleJpaRepository.save(savedArticle);
        entityManager.flush();
        entityManager.clear();

        // then
        Article updatedArticle = articleJpaRepository
                .findById(savedArticle.getId())
                .orElseThrow();
        assertThat(updatedArticle.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedArticle.getDescription()).isEqualTo("Updated description");
        assertThat(updatedArticle.getContent()).isEqualTo("Updated body");
        assertThat(updatedArticle.getSlug()).isEqualTo("updated-title");
    }

    @Test
    @DisplayName("Should delete article")
    void whenDeleteArticle_thenArticleIsRemoved() {
        // given
        Article article = new Article(testAuthor, "Test Article", "Test description", "Test body");
        Article savedArticle = articleJpaRepository.save(article);
        entityManager.flush();

        // when
        articleJpaRepository.delete(savedArticle);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(articleJpaRepository.findById(savedArticle.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple articles from different authors")
    void whenMultipleAuthorsExist_thenCanFilterByAuthor() {
        // given
        User anotherAuthor = new User("another@example.com", "another", "password123");
        entityManager.persist(anotherAuthor);

        Article article1 = new Article(testAuthor, "Author 1 Article", "Description", "Body");
        Article article2 = new Article(anotherAuthor, "Author 2 Article", "Description", "Body");
        articleJpaRepository.saveAll(List.of(article1, article2));
        entityManager.flush();
        entityManager.clear();

        // when
        Page<Article> authorArticles = articleJpaRepository.findByAuthorInOrderByCreatedAtDesc(
                List.of(testAuthor), PageRequest.of(0, 10));

        // then
        assertThat(authorArticles.getContent()).hasSize(1);
        assertThat(authorArticles.getContent().get(0).getTitle())
                .isEqualTo("Author 1 Article");
    }
}
