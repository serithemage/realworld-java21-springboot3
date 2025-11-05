package io.zhc1.realworld.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import io.zhc1.realworld.config.AuthTokenProvider;
import io.zhc1.realworld.model.Article;
import io.zhc1.realworld.model.ArticleDetails;
import io.zhc1.realworld.model.ArticleFacets;
import io.zhc1.realworld.model.User;
import io.zhc1.realworld.service.ArticleService;
import io.zhc1.realworld.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ArticleController - Article Management API")
class ArticleControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @MockBean
    ArticleService articleService;

    @Autowired
    AuthTokenProvider authTokenProvider;

    private User createUserWithId(UUID id, String email, String username, String password) throws Exception {
        User user = new User(email, username, password);
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }

    private Article createArticleWithSlug(User author, String slug, String title) throws Exception {
        Article article = new Article(author, title, "description", "body");
        Field slugField = Article.class.getDeclaredField("slug");
        slugField.setAccessible(true);
        slugField.set(article, slug);
        Field idField = Article.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(article, 1);
        return article;
    }

    // NOTE: POST /api/articles test requires complex Article-Tag relationship setup
    // This is better tested with integration tests using real database

    @Test
    @DisplayName("GET /api/articles - Should return articles for authenticated user")
    void whenGetArticlesWithAuthentication_thenShouldReturn200Ok() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        User user = createUserWithId(userId, "test@example.com", "testuser", "password123");
        String token = authTokenProvider.createAuthToken(user);

        Article article = createArticleWithSlug(user, "test-article", "Test Article");
        ArticleDetails articleDetails = new ArticleDetails(article, 0, false);

        when(userService.getUser(userId)).thenReturn(user);
        when(articleService.getArticles(any(User.class), any(ArticleFacets.class)))
                .thenReturn(List.of(articleDetails));

        // when & then
        mockMvc.perform(get("/api/articles").header("Authorization", "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articles").isArray())
                .andExpect(jsonPath("$.articles[0].slug").value("test-article"));
    }

    @Test
    @DisplayName("GET /api/articles - Should return articles for anonymous user")
    void whenGetArticlesWithoutAuthentication_thenShouldReturn200Ok() throws Exception {
        // given
        User author = createUserWithId(UUID.randomUUID(), "author@example.com", "author", "password123");
        Article article = createArticleWithSlug(author, "test-article", "Test Article");
        ArticleDetails articleDetails = new ArticleDetails(article, 0, false);

        when(articleService.getArticles(any(ArticleFacets.class))).thenReturn(List.of(articleDetails));

        // when & then
        mockMvc.perform(get("/api/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articles").isArray())
                .andExpect(jsonPath("$.articles[0].slug").value("test-article"));
    }

    @Test
    @DisplayName("GET /api/articles/{slug} - Should return article for authenticated user")
    void whenGetArticleWithAuthentication_thenShouldReturn200Ok() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        User user = createUserWithId(userId, "test@example.com", "testuser", "password123");
        String token = authTokenProvider.createAuthToken(user);

        String slug = "test-article";
        Article article = createArticleWithSlug(user, slug, "Test Article");
        ArticleDetails articleDetails = new ArticleDetails(article, 0, false);

        when(articleService.getArticle(slug)).thenReturn(article);
        when(userService.getUser(userId)).thenReturn(user);
        when(articleService.getArticleDetails(user, article)).thenReturn(articleDetails);

        // when & then
        mockMvc.perform(get("/api/articles/{slug}", slug).header("Authorization", "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.article.slug").value(slug));
    }

    @Test
    @DisplayName("GET /api/articles/{slug} - Should return article for anonymous user")
    void whenGetArticleWithoutAuthentication_thenShouldReturn200Ok() throws Exception {
        // given
        String slug = "test-article";
        User author = createUserWithId(UUID.randomUUID(), "author@example.com", "author", "password123");
        Article article = createArticleWithSlug(author, slug, "Test Article");
        ArticleDetails articleDetails = new ArticleDetails(article, 0, false);

        when(articleService.getArticle(slug)).thenReturn(article);
        when(articleService.getArticleDetails(article)).thenReturn(articleDetails);

        // when & then
        mockMvc.perform(get("/api/articles/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.article.slug").value(slug));
    }

    @Test
    @DisplayName("PUT /api/articles/{slug} - Should update article successfully")
    void whenUpdateArticle_thenShouldReturn200Ok() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        User user = createUserWithId(userId, "test@example.com", "testuser", "password123");
        String token = authTokenProvider.createAuthToken(user);

        String slug = "test-article";
        Article article = createArticleWithSlug(user, slug, "Test Article");
        Article updatedArticle = createArticleWithSlug(user, "updated-article", "Updated Article");
        ArticleDetails articleDetails = new ArticleDetails(updatedArticle, 0, false);

        String requestBody =
                """
                {
                    "article": {
                        "title": "Updated Article",
                        "description": "Updated description",
                        "body": "Updated body"
                    }
                }
                """;

        when(userService.getUser(userId)).thenReturn(user);
        when(articleService.getArticle(slug)).thenReturn(article);
        when(articleService.editTitle(any(User.class), any(Article.class), anyString()))
                .thenReturn(updatedArticle);
        when(articleService.editDescription(any(User.class), any(Article.class), anyString()))
                .thenReturn(updatedArticle);
        when(articleService.editContent(any(User.class), any(Article.class), anyString()))
                .thenReturn(updatedArticle);
        when(articleService.getArticleDetails(user, updatedArticle)).thenReturn(articleDetails);

        // when & then
        mockMvc.perform(put("/api/articles/{slug}", slug)
                        .header("Authorization", "Token " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.article.title").value("Updated Article"));
    }

    @Test
    @DisplayName("DELETE /api/articles/{slug} - Should delete article successfully")
    void whenDeleteArticle_thenShouldReturn200Ok() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        User user = createUserWithId(userId, "test@example.com", "testuser", "password123");
        String token = authTokenProvider.createAuthToken(user);

        String slug = "test-article";
        Article article = createArticleWithSlug(user, slug, "Test Article");

        when(userService.getUser(userId)).thenReturn(user);
        when(articleService.getArticle(slug)).thenReturn(article);
        doNothing().when(articleService).delete(any(User.class), any(Article.class));

        // when & then
        mockMvc.perform(delete("/api/articles/{slug}", slug).header("Authorization", "Token " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/articles/feed - Should return article feeds successfully")
    void whenGetArticleFeeds_thenShouldReturn200Ok() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        User user = createUserWithId(userId, "test@example.com", "testuser", "password123");
        String token = authTokenProvider.createAuthToken(user);

        Article article = createArticleWithSlug(user, "test-article", "Test Article");
        ArticleDetails articleDetails = new ArticleDetails(article, 0, false);

        when(userService.getUser(userId)).thenReturn(user);
        when(articleService.getFeeds(any(User.class), any(ArticleFacets.class)))
                .thenReturn(List.of(articleDetails));

        // when & then
        mockMvc.perform(get("/api/articles/feed").header("Authorization", "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articles").isArray())
                .andExpect(jsonPath("$.articles[0].slug").value("test-article"));
    }

    @Test
    @DisplayName("POST /api/articles - Should fail without authentication token")
    void whenPostArticleWithoutToken_thenShouldReturn401Unauthorized() throws Exception {
        // given
        String requestBody =
                """
                {
                    "article": {
                        "title": "Test Article",
                        "description": "Test description",
                        "body": "Test body"
                    },
                    "tags": ["java", "spring"]
                }
                """;

        // when & then
        mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/articles/{slug} - Should fail without authentication token")
    void whenUpdateArticleWithoutToken_thenShouldReturn401Unauthorized() throws Exception {
        // given
        String requestBody =
                """
                {
                    "article": {
                        "title": "Updated Article"
                    }
                }
                """;

        // when & then
        mockMvc.perform(put("/api/articles/{slug}", "test-article")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/articles/{slug} - Should fail without authentication token")
    void whenDeleteArticleWithoutToken_thenShouldReturn401Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/articles/{slug}", "test-article"))
                .andExpect(status().isUnauthorized());
    }

    // NOTE: Authentication tests for /api/articles/feed are covered by Spring Security integration tests
}
