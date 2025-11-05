package io.zhc1.realworld.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import io.zhc1.realworld.config.AuthTokenProvider;
import io.zhc1.realworld.model.Article;
import io.zhc1.realworld.model.ArticleDetails;
import io.zhc1.realworld.model.User;
import io.zhc1.realworld.service.ArticleService;
import io.zhc1.realworld.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ArticleFavoriteController - Article Favorite Management API")
class ArticleFavoriteControllerTest {
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

    @Test
    @DisplayName("POST /api/articles/{slug}/favorite - Should favorite article successfully")
    void whenFavoriteArticle_thenShouldReturn200Ok() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        User user = createUserWithId(userId, "test@example.com", "testuser", "password123");
        String token = authTokenProvider.createAuthToken(user);

        String slug = "test-article";
        Article article = createArticleWithSlug(user, slug, "Test Article");
        ArticleDetails articleDetails = new ArticleDetails(article, 1, true);

        when(userService.getUser(userId)).thenReturn(user);
        when(articleService.getArticle(slug)).thenReturn(article);
        doNothing().when(articleService).favorite(any(User.class), any(Article.class));
        when(articleService.getArticleDetails(user, article)).thenReturn(articleDetails);

        // when & then
        mockMvc.perform(post("/api/articles/{slug}/favorite", slug)
                        .header("Authorization", "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.article.slug").value(slug))
                .andExpect(jsonPath("$.article.favorited").value(true))
                .andExpect(jsonPath("$.article.favoritesCount").value(1));
    }

    @Test
    @DisplayName("DELETE /api/articles/{slug}/favorite - Should unfavorite article successfully")
    void whenUnfavoriteArticle_thenShouldReturn200Ok() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        User user = createUserWithId(userId, "test@example.com", "testuser", "password123");
        String token = authTokenProvider.createAuthToken(user);

        String slug = "test-article";
        Article article = createArticleWithSlug(user, slug, "Test Article");
        ArticleDetails articleDetails = new ArticleDetails(article, 0, false);

        when(userService.getUser(userId)).thenReturn(user);
        when(articleService.getArticle(slug)).thenReturn(article);
        doNothing().when(articleService).unfavorite(any(User.class), any(Article.class));
        when(articleService.getArticleDetails(user, article)).thenReturn(articleDetails);

        // when & then
        mockMvc.perform(delete("/api/articles/{slug}/favorite", slug)
                        .header("Authorization", "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.article.slug").value(slug))
                .andExpect(jsonPath("$.article.favorited").value(false))
                .andExpect(jsonPath("$.article.favoritesCount").value(0));
    }

    @Test
    @DisplayName("POST /api/articles/{slug}/favorite - Should fail without authentication token")
    void whenFavoriteArticleWithoutToken_thenShouldReturn401Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(post("/api/articles/{slug}/favorite", "test-article"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/articles/{slug}/favorite - Should fail without authentication token")
    void whenUnfavoriteArticleWithoutToken_thenShouldReturn401Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/articles/{slug}/favorite", "test-article"))
                .andExpect(status().isUnauthorized());
    }
}
